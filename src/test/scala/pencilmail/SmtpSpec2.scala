package pencilmail

import cats.effect.IO
import org.specs2.specification.core.Fragments
import org.specs2.specification.{AfterSpec, BeforeSpec}
import pencilmail.SmtpSpec2.mimeEmail
import pencilmail.data.{Body, Email}
import pencilmail.lib.MailServerSpec
import pencilmail.syntax.*

import scala.language.implicitConversions
class SmtpSpec2 extends MailServerSpec with BeforeSpec with AfterSpec {

  sequential
  
  override def beforeSpec: Fragments = step(start())
  override def afterSpec: Fragments = step(stop())
  
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
