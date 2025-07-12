package pencilmail

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.option.*
import cats.syntax.show.*
import com.comcast.ip4s.*
import fs2.io.net.Network
import org.http4s.Charset
import org.specs2.mutable.SpecificationLike
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pencilmail.data.*
import pencilmail.syntax.*

class SmtpSESSpec extends SpecificationLike with LiteralsSyntax {
  val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLoggerFromName[IO]("smtp.integration.test")

  "Smtp integration" should {
    "send text email through AWS SES email service" in {
      val username = sys.props.get("aws.username")
      val password = sys.props.get("aws.password")
      val from = sys.props.get("aws.from")
      val to = sys.props.get("aws.to")

      if (username.isEmpty || password.isEmpty || from.isEmpty || to.isEmpty) {
        logger.warn("Skip SES test. You must provider aws.username, aws.password, aws.from, aws.to JVM parameters.").unsafeRunSync()
        skipped
      } else {
        logger.debug(s"Username: $username - Password: $password - From: $from - To: $to").unsafeRunSync()
        val sesEmail: Email = Email.mime(
          From(Mailbox.unsafeFromString(s"привет <${from.get}>")),
          To(Mailbox.unsafeFromString(s"<${to.get}>")),
          Subject("привет"),
          Body.Html(java.nio.file.Files.readString(Files.pathFromClassLoader[IO]("rich.html").unsafeRunSync(), Charset.`UTF-8`.nioCharset)),
          List(Attachment.fromString[IO]("files/jpeg-sample.jpg").unsafeRunSync())
        )
        val sendEmail = for {
          tls      <- Network[IO].tlsContext.system
          client    = Client[IO](
                        SocketAddress(host"email-smtp.us-east-1.amazonaws.com", port"587"),
                        mode = SmtpMode.StartTLS,
                        Credentials(
                          Username(username.get),
                          Password(password.get)
                        ).some
                      )(tls, logger)
          response <- client.send(sesEmail)
        } yield response

        sendEmail.attempt.unsafeRunSync() match {
          case Right(value)     =>
            success(value.show)
          case Left(err: Error) =>
            failure(err.show)
          case Left(e)          => failure(e.getLocalizedMessage)
        }
      }
    }
  }
}
