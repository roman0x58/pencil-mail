package pencilmail.lib

import cats.effect.{Deferred, IO, Ref}
import com.comcast.ip4s.*
import fs2.Stream
import fs2.interop.scodec.{StreamDecoder, StreamEncoder}
import fs2.io.net.{Network, Socket}
import pencilmail.protocol.Command.*
import pencilmail.protocol.{Command, Replies}
import scodec.Codec
import scodec.bits.BitVector

type ServerState = (bits: BitVector, command: Command, reply: Replies)

final case class SmtpServer(
    state: Ref[IO, List[ServerState]]
) {

  def start(localBindAddress: Deferred[IO, SocketAddress[Host]]): IO[Unit] =
    Stream
      .resource(Network[IO].serverResource(Some(ip"127.0.0.1"), Some(port"0")))
      .flatMap { case (localAddress, server) =>
        Stream.eval(localBindAddress.complete(localAddress)).drain ++
          server.flatMap { socket =>
            val sock = MessageSocket(socket)
            sock.write(DataSamples.`220 Greeting`) ++ sock.read.through(processCommand).through(sock.writes)
          }
      }
      .compile
      .drain

  def processCommand(stream: Stream[IO, In]): Stream[IO, Replies] = {
    def process(bitVector: BitVector, cmd: Command, replies: Replies) =
      Stream.eval(state.tryUpdate(_ :+ (bitVector, cmd, replies))).drain ++ Stream(replies)

    stream.flatMap {
      case In(raw, cmd @ Ehlo(_)) =>
        process(raw, cmd, DataSamples.ehloReplies)
      case In(raw, cmd @ Mail(_)) =>
        process(raw, cmd, DataSamples.`250 OK`)
      case In(raw, cmd @ Rcpt(_)) =>
        process(raw, cmd, DataSamples.`250 OK`)

      case In(raw, cmd @ Data) =>
        process(raw, cmd, DataSamples.`354 End data`)

      case In(raw, cmd @ Quit) =>
        process(raw, cmd, DataSamples.`221 Buy`)

      case In(raw, cmd @ Noop) =>
        process(raw, cmd, DataSamples.`250 OK`)

      case In(raw, cmd @ Rset) =>
        process(raw, cmd, DataSamples.`250 OK`)

      case In(raw, cmd @ Vrfy(_)) =>
        process(raw, cmd, DataSamples.`250 OK`)

      case In(raw, cmd @ Text(Command.endEmail)) =>
        process(raw, cmd, DataSamples.`250 OK`)

      case In(raw, cmd @ Text(_)) =>
        Stream
          .eval(state.get)
          .flatMap(_.lastOption.map(_.reply) match {
            case Some(r @ DataSamples.`334_USERNAME`) =>
              process(raw, cmd, DataSamples.`334_PASSWORD`)
            case Some(r @ DataSamples.`334_PASSWORD`) =>
              process(raw, cmd, DataSamples.`235 AUTH OK`)
            case _ =>
              Stream.eval(state.tryUpdate(_ :+ (raw, cmd, Replies.empty))).drain ++ Stream.empty
          })
      case In(raw, cmd @ AuthLogin) =>
        process(raw, cmd, DataSamples.`334_USERNAME`)
      case In(raw, cmd @ StartTls) =>
        process(raw, cmd, DataSamples.`220 Greeting`)
    }
  }

}

final case class MessageSocket(socket: Socket[IO]) extends Product with Serializable {
  def read: Stream[IO, In] =
    socket.reads
      .through(decoder.toPipeByte[IO])
      .through { s =>
        s.flatMap(Stream.emits(_))
      }
  def write(replies: Replies): Stream[IO, Unit] =
    writes(Stream.emit(replies))

  def writes(stream: Stream[IO, Replies]): Stream[IO, Unit] =
    stream
      .through(StreamEncoder.many(summon[Codec[Replies]]).toPipeByte)
      .through(socket.writes)

  val decoder: StreamDecoder[List[In]] =
    StreamDecoder.many(In.inListDecoder)

}
