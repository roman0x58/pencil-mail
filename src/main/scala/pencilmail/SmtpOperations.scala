package pencilmail

import cats.data.Kleisli
import cats.effect.{Async, Resource}
import org.typelevel.log4cats.Logger
import pencilmail.data.*
import pencilmail.protocol.Replies

type SMTPOperation[F[_]] = (tlsSocketResource: Resource[F, SmtpSocket[F]]) => Smtp[F, Replies]

class SmtpOperations[F[_]: {Logger, Async}](val credentials: Option[Credentials]) {
  val logger: Logger[F] = Logger[F]

  def tlsOperation: SMTPOperation[F] = tlsSocketResource =>
    Smtp[F] { req =>
      tlsSocketResource.use(tls =>
        (for {
          rep      <- Smtp.ehlo[F]()
          _        <- maybeAuthenticate(rep)
          response <- sender
        } yield response).run(req.copy(socket = tls))
      )
    }

  private def maybeAuthenticate(replies: Replies): Smtp[F, Unit] =
    if (replies.isAuthSupported) credentials.fold(Smtp.unit)(Smtp.login[F])
    else Smtp.unit[F]

  private def sender: Smtp[F, Replies] = Smtp.ask[F].flatMap { r =>
    r.email match {
      case Email(_, _, _, _, _, _, EmailType.Text) =>
        for
          _ <- Smtp.mail[F]()
          _ <- Smtp.rcpt[F]()
          _ <- Smtp.data[F]()
          _ <- Smtp.mainHeaders[F]()
          _ <- Smtp.emptyLine[F]()
          _ <- Smtp.asciiBody[F]()
          r <- Smtp.quit[F]()
        yield r

      case Email(_, _, _, _, _, _, EmailType.Mime(_, _)) =>
        for
          _ <- Smtp.mail[F]()
          _ <- Smtp.rcpt[F]()
          _ <- Smtp.data[F]()
          _ <- Smtp.mimeHeader[F]()
          _ <- Smtp.mainHeaders[F]()
          _ <- Smtp.multipart[F]()
          _ <- Smtp.emptyLine[F]()
          _ <- Smtp.mimeBody[F]()
          _ <- Smtp.attachments[F]()
          _ <- Smtp.endEmail[F]()
          r <- Smtp.quit[F]()
        yield r
    }
  }

  def startTlsOperation: SMTPOperation[F] = tlsSocketResource =>
    for
      _   <- Smtp.init[F]()
      _   <- Smtp.ehlo[F]()
      _   <- Smtp.startTls[F]()
      rep <- tlsOperation(tlsSocketResource)
    yield rep

  def plainOperation: SMTPOperation[F] = _ =>
    for
      _        <- Smtp.init[F]()
      rep      <- Smtp.ehlo[F]()
      _        <- {
        if (rep.supportTLS)
          Smtp.liftF(Async[F].raiseError(Error.PencilError("Use TLS mode instead of plain.")))
        else maybeAuthenticate(rep)
      }
      response <- sender
    yield response
}
