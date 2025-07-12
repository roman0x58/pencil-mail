
package pencilmail.lib

import pencilmail.data.{Body, Email}
import pencilmail.syntax.LiteralsSyntax

object Fixtures extends LiteralsSyntax: 
  val mimeEmail: Email = Email.mime(
    from"kaspar minosyants<user1@mydomain.tld>",
    to"pencil <pencil@mail.pencil.com>",
    subject"привет",
    Body.Alternative(List(Body.Utf8("hi there3"), Body.Ascii("hi there2")))
    /// List(attachment"/home/kaspar/stuff/sources/pencil/src/test/resources/files/jpeg-sample.jpg")
  ) + cc"<foo@bar.com>" + bcc"<foo@bar.com>"
  val mimeEmail2: Email = Email.mime(
    from"kaspar minosyants<user1@mydomain.tld>",
    to"pencil <pencil@mail.pencil.com>",
    subject"привет",
    Body.Ascii("hi there2")
    //  List(attachment"/home/kaspar/stuff/sources/pencil/src/test/resources/files/jpeg-sample.jpg")
  ) + cc"<foo@bar.com>" + bcc"<foo@bar.com>"
  
  extension (email: Email)
    def bccAddresses: List[String] = {
      email.bcc.toList.flatMap(_.toList.map(_.address))
    }
    def toAddresses: List[String] = {
      email.to.toList.map(_.address)
    }
    def ccAddresses: List[String] = {
      email.cc.toList.flatMap(_.toList.map(_.address))
    }
