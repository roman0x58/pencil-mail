package pencilmail

import cats.effect.IO
import pencilmail.syntax.*
import pencilmail.protocol.Code
import cats.syntax.flatMap.*
import pencilmail.data.{Body, Email}
import SmtpSpec2.mimeEmail
import org.specs2.specification.{AfterAll, BeforeAll}
import pencilmail.lib.MailServerSpec
class SmtpSpec2 extends MailServerSpec with BeforeAll with AfterAll {

  sequential
  
  override def beforeAll(): Unit = start()
  override def afterAll(): Unit = stop()
  
  "smtp command be" should {
    "ehlo" in {
      val r = Smtp.ehlo[IO]().runCommand
      r.replies.head.code.success
    }
    "noop" in {
      val r = Smtp.noop[IO]().runCommand
      r.replies.head.code.success
    }
    "quit" in {
      val r = Smtp.quit[IO]().runCommand
      r.replies.head.code.success
    }

  }
}

object SmtpSpec2 extends LiteralsSyntax:
  given mimeEmail: Email = Email.mime(
    from"kaspar minosyants<user1@mydomain.tld>",
    to"pencil <pencil@mail.pencil.com>",
    subject"привет",
    Body.Utf8("hi there"),
    List(attachment"/home/kaspar/stuff/sources/pencil/src/test/resources/files/jpeg-sample.jpg")
  )
