package pencilmail

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.option.*
import fs2.io.net.Network
import io.circe.Decoder
import io.circe.generic.auto.*
import org.http4s.EntityDecoder
import org.http4s.circe.*
import org.http4s.ember.client.EmberClientBuilder
import org.specs2.matcher.MatchResult
import pencilmail.SmtpMode.{Plain, StartTLS, TLS}
import pencilmail.data.*
import pencilmail.lib.*
import pencilmail.lib.Fixtures.*
import pencilmail.protocol.Replies

class SendEmailSpec extends MailServerSpec {
  sequential

  "email" should {
    "send mime email Plain" in useContainer(
      ContainerConfiguration(
        smtpMode = Plain
      )
    ) {
      val email   = Fixtures.mimeEmail2
      val message = execute(email, Plain).unsafeRunSync()
      assertMail(email, message)
    }
    "send mime email Start TLS" in useContainer(
      ContainerConfiguration(
        smtpMode = StartTLS
      )
    ) {
      val email   = Fixtures.mimeEmail2
      val message = execute(email, StartTLS).unsafeRunSync()
      assertMail(email, message)
    }
    "send mime email TLS" in useContainer(
      ContainerConfiguration(
        smtpMode = TLS
      )
    ) {
      val email   = Fixtures.mimeEmail2
      val message = execute(email, TLS).unsafeRunSync()

      assertMail(email, message)
    }
  }

  def assertMail(email: Email, message: Message2): MatchResult[Any] = {
    def addresses(opt: Option[Seq[MailBox]]) = {
      val mailboxes: Seq[MailBox] = opt.toList.flatten
      mailboxes.map(_.Address)
    }

    addresses(message.Bcc) must containTheSameElementsAs(email.bccAddresses)
    addresses(message.Cc) must containTheSameElementsAs(email.ccAddresses)
    addresses(message.To.some) must containTheSameElementsAs(email.toAddresses)

    message.From.Address must_== email.from.address
    Name(message.From.Name) must_== email.from.mailbox.name.get
    message.Subject must_== email.subject.get.asString
    message.Text.trim must_== email.body.flatMap(_.body).get
  }

  def execute(email: Email, mode: SmtpMode): IO[Message2] = {
    def sendEmail(email: Email): IO[Replies] =
      for
        tls       <- Network[IO].tlsContext.fromKeyStoreFile(
                       Files.pathFromClassLoader[IO]("certs/test-truststore.jks").unsafeRunSync(),
                       "changeit".toCharArray,
                       "pencil1234".toCharArray
                     )
        smtpClient = Client[IO](container.socketAddress(), mode, Some(container.credentials), tls, logger)
        response  <- smtpClient.send(email)
      yield response
    EmberClientBuilder
      .default[IO]
      .build
      .use { httpClient =>
        for
          _        <- sendEmail(Fixtures.mimeEmail2)
          messages <- httpClient.expect[Messages](
                        s"""http://localhost:${container.httpPort}/api/v1/messages"""
                      )
          id        = messages.messages.head.ID
          message  <- httpClient.expect[Message2](
                        s"""http://localhost:${container.httpPort}/api/v1/message/$id"""
                      )
        yield message
      }
  }
}

final case class MailBox(Name: String, Address: String)

final case class Message(
    ID: String,
    From: MailBox,
    To: List[MailBox],
    Cc: Option[List[MailBox]],
    Bcc: Option[List[MailBox]],
    Subject: String,
    Attachments: Int
)
final case class Message2(
    ID: String,
    From: MailBox,
    To: List[MailBox],
    Cc: Option[List[MailBox]],
    Bcc: Option[List[MailBox]],
    Subject: String,
    Text: String,
    HTML: String
)

object Message2:
  given EntityDecoder[IO, Message2] = jsonOf[IO, Message2]

object Message:
  given mbDecoder: Decoder[Mailbox] = Decoder.decodeString.map(Mailbox.unsafeFromString)
  given EntityDecoder[IO, MailBox]  = jsonOf[IO, MailBox]
  given EntityDecoder[IO, Message]  = jsonOf[IO, Message]

final case class Messages(messages: List[Message])
object Messages:
  given EntityDecoder[IO, Messages] = jsonOf[IO, Messages]
