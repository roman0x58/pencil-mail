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

import cats.effect.{Async, Concurrent, Resource}
import cats.syntax.applicativeError.*
import cats.syntax.apply.*
import cats.syntax.show.*
import com.comcast.ip4s.*
import fs2.io.net.tls.{TLSContext, TLSParameters}
import fs2.io.net.{Network, Socket}
import org.typelevel.log4cats.Logger
import pencilmail.data.*
import pencilmail.protocol.*

/** Smtp client
  */
trait Client[F[_]] {

  /** Sends `email` to a smtp server
    *
    * @param email
    *   \- email to be sent
    * @return
    *   \- IO of [[Replies]] from smtp server
    */
  def send(email: Email): F[Replies]

}

object Client:
  def apply[F[_]: {Async, Concurrent, Network}](
      address: SocketAddress[Host],
      mode: SmtpMode,
      credentials: Option[Credentials],
      tlsContext: TLSContext[F],
      logger: Logger[F],
      tlsParameters: TLSParameters = TLSParameters.Default,
      socketTimeoutConfig: SocketTimeoutConfig = SocketTimeoutConfig.default
  ): Client[F] =
    new Client[F] {
      private val socketResource: Resource[F, Socket[F]] = Network[F].client(address)

      given Logger[F] = logger

      val operations: SmtpOperations[F] = SmtpOperations[F](credentials)

      override def send(email: Email): F[Replies] = {
        val sockets = for
          socket <- socketResource
          tls     = tlsContext
                      .clientBuilder(socket)
                      .withParameters(tlsParameters)
                      .build
                      .map(SmtpSocket.fromSocket(_, socketTimeoutConfig))
        yield (SmtpSocket.fromSocket(socket, socketTimeoutConfig), tls)

        sockets
          .use { case (socket, tlsSocket) =>
            val request = mode match {
              case SmtpMode.Plain    =>
                operations.plainOperation(tlsSocket)
              case SmtpMode.StartTLS =>
                operations.startTlsOperation(tlsSocket)
              case SmtpMode.TLS      =>
                operations.tlsOperation(tlsSocket)
            }
            (Smtp.liftF(logger.debug(s"Sending email using SMTP mode: $mode to address $address")) *> request)
              .run(SmtpRequest(email, socket))
          }
          .recoverWith {
            case e: Error     =>
              logger.error(e)(s"SMTP error occurred while sending email message. ${e.show}") *> Async[F].raiseError(e)
            case e: Throwable =>
              logger.error(e)("An error occurred while sending email message") *> Async[F].raiseError(e)
          }
      }
    }
