package pencilmail.lib

import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.instances.list.*
import cats.syntax.flatMap.*
import cats.syntax.traverse.*
import com.comcast.ip4s.{Host, SocketAddress}
import fs2.io.net.Network
import org.specs2.mutable.SpecificationLike
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import pencilmail.data.*
import pencilmail.lib.SmtpServer
import pencilmail.protocol.{Command, Replies}
import pencilmail.syntax.LiteralsSyntax
import pencilmail.{Password, Smtp, SmtpRequest, SmtpSocket, Username, Host as PHost}
import scodec.bits.BitVector
import scodec.{Codec, DecodeResult}
import cats.syntax.either.*

import java.time.{Clock, Instant, ZoneId, ZoneOffset}
import java.util.UUID
import scala.concurrent.duration.*

trait SmtpBaseSpec extends SpecificationLike with LiteralsSyntax:

  given logger: Logger[IO] = Slf4jLogger.getLoggerFromName[IO]("smtp.base")
  val timestamp: Instant = Instant.now()
  val clock: Clock = Clock.fixed(timestamp, ZoneId.from(ZoneOffset.UTC))
  val host: HostType.Host = PHost.local()
  val uuid: String = UUID.randomUUID().toString

  protected def credentials: Credentials = Credentials(
    Username("pencilmail"),
    Password("pencil1234")
  )
  def socket(
      address: SocketAddress[Host]
  ): Resource[IO, SmtpSocket[IO]] =
    Network[IO]
      .client(address)
      .map(SmtpSocket.fromSocket(_))

  type ServerStateRef = Ref[IO, List[ServerState]]
  def withSocket[A](
      run: (SmtpSocket[IO], ServerStateRef, SocketAddress[Host]) => IO[A]
  ): IO[A] =
    for {
      localBindAddress <- Deferred[IO, SocketAddress[Host]]
      state <- Ref[IO].of(List.empty[ServerState])
      server <- SmtpServer(state).start(localBindAddress).start
      address <- localBindAddress.get
      result <- socket(address).use(sock => run(sock, state, address))
      _ <- server.cancel
    } yield result


  def runTestCommandWithSocket[Repl, B](
      command: (socket: Resource[IO, SmtpSocket[IO]]) => Smtp[IO, Repl],
      email: Email,
      codec: Codec[B]
  ): Either[Throwable, (reply: Repl, response: List[(decoded: B, cmd: Command, reply: Replies)])] =
    withSocket { (smtpSocket, state, address) =>
      (for {
        lastCommand <- command(Resource.pure(smtpSocket))
        raw <- Smtp.liftF(Temporal[IO].sleep(100.millis) >> state.get)
        result <- Smtp.liftF(raw.traverse { case (bits, req, rep) =>
          codec.decode(bits).toEither match {
            case Right(DecodeResult(decoded, _)) =>
              IO.pure((decoded, req, rep))
            case Left(err) =>
              Error.smtpError[IO, (B, Command, Replies)](err.message)
          }
        })
      } yield (lastCommand, result)).run(
        SmtpRequest(
          email,
          smtpSocket,
          PHost.local(),
          Instant.now(clock),
          () => uuid
        )
      )
    }.attempt.unsafeRunSync()


  /** @deprecated
    */
  def runTestCommand[A, B](
      command: Smtp[IO, A],
      email: Email,
      codec: Codec[B]
  ): Either[Throwable, (A, List[B])] =
    runTestCommandWithSocket(_ => Smtp.init[IO]() >> command, email, codec).map { case (a, b) => (a, b.map(_.decoded)) }
