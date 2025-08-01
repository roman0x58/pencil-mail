/*
 * Copyright 2020 Kaspar Minosiants
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pencilmail

import cats.*
import cats.effect.kernel.Async
import cats.implicits.*
import fs2.Chunk
import fs2.io.net.Socket
import fs2.io.net.tls.TLSSocket
import org.typelevel.log4cats.Logger
import pencilmail.data.Error
import pencilmail.protocol.*
import scodec.bits.BitVector
import scodec.codecs.*
import scodec.{Attempt, Codec, DecodeResult}

import scala.concurrent.duration.*

final class SocketTimeoutConfig private (
    val socketReadTimeout: FiniteDuration,
    val socketWriteTimeout: FiniteDuration
) { self =>
  def withSocketWriteTimeout(socketReadTimeout: FiniteDuration): SocketTimeoutConfig =
    copy(socketReadTimeout = socketReadTimeout)
  def withSocketReadTimeout(socketReadTimeout: FiniteDuration): SocketTimeoutConfig  =
    copy(socketReadTimeout = socketReadTimeout)

  private def copy(
      socketReadTimeout: FiniteDuration = self.socketReadTimeout,
      socketWriteTimeout: FiniteDuration = self.socketWriteTimeout
  ): SocketTimeoutConfig = new SocketTimeoutConfig(
    socketReadTimeout = socketReadTimeout,
    socketWriteTimeout = socketWriteTimeout
  )
}

object SocketTimeoutConfig {
  def default = new SocketTimeoutConfig(
    socketReadTimeout = 10.seconds,
    socketWriteTimeout = 10.seconds
  )
}

/** Wraps [[Socket[IO]]] with smtp specific protocol
  */
trait SmtpSocket[F[_]]:

  /** Reads [[Replies]] from smtp server
    */
  def read(): F[Replies]

  /** Send [[Command]] to smtp server
    */
  def write(command: Command): F[Unit]

object SmtpSocket:
  def fromSocket[F[_]: {Async, Logger}](
      underlying: Socket[F],
      timeoutConfig: SocketTimeoutConfig = SocketTimeoutConfig.default
  )(using c: Codec[Replies]): SmtpSocket[F] = new SmtpSocket[F] {
    val logger: Logger[F] = Logger[F]

    private val socketName = underlying match {
      case socket: TLSSocket[_] => "TLS Socket"
      case _                    => "Plain Socket"
    }

    override def toString: String = socketName

    def bytesToReply(bytes: Array[Byte]): F[Replies] =
      c.decode(BitVector(bytes)) match
        case Attempt.Successful(DecodeResult(value, _)) =>
          logger.debug(s"[$socketName] Getting Replies: ${value.show}") *>
            Applicative[F].pure(value)

        case Attempt.Failure(cause) =>
          logger.debug(s"[$socketName] Getting Error: ${cause.messageWithContext}") *>
            Error.smtpError[F, Replies](cause.messageWithContext)

    override def read(): F[Replies] =
      Async[F].timeoutTo(
        underlying.read(8192).flatMap {
          case Some(chunk) => bytesToReply(chunk.toArray)
          case None        => Error.smtpError[F, Replies]("Nothing to read")
        },
        timeoutConfig.socketReadTimeout,
        Error.timeout[F, Replies](
          s"Socket read timeout error. Timeout time exceeded: ${timeoutConfig.socketReadTimeout}"
        )
      )

    override def write(command: Command): F[Unit] =
      utf8.encode(command.show) match
        case Attempt.Successful(value) =>
          Async[F].timeoutTo(
            logger.debug(s"[$socketName] Sending command: ${command.show}") *>
              underlying.write(Chunk.array(value.toByteArray)),
            timeoutConfig.socketWriteTimeout,
            Error.timeout[F, Unit](
              s"Socket write timeout error. Timeout time exceeded: ${timeoutConfig.socketWriteTimeout}"
            )
          )
        case Attempt.Failure(cause)    =>
          logger.error(
            s"An error occurred while writing command to socket ${command.show}. Cause: ${cause.messageWithContext}"
          ) *> Error.smtpError[F, Unit](cause.messageWithContext)

  }
