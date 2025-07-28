package pencilmail.lib

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.either.*
import cats.syntax.show.*
import fs2.io.net.Network
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.SpecificationLike
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pencilmail.data.*
import pencilmail.protocol.Replies
import pencilmail.syntax.*
import pencilmail.{Smtp, SmtpRequest, SmtpSocket}

trait MailServerSpec extends SpecificationLike with LiteralsSyntax:
  given logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
  var container_ : Option[MailServerContainer] = None

  def container: MailServerContainer = container_.get
  def useContainer[T: AsResult](configuration: ContainerConfiguration = ContainerConfiguration())(t: => T): Result = {
      try {
        start(configuration)
        AsResult(t)
      } finally {
        stop()
      }
  }

  protected def start(conf: ContainerConfiguration = ContainerConfiguration()): Unit = {
    container_ = Some(MailServerContainer.mk(conf))
    container_.foreach(_.start())
  }

  protected def stop(): Unit = {
    container_.foreach(_.stop())
  }


  def runC[R](command: Smtp[IO, R])(using email: Email): IO[R] =
    Network[IO].client(container.socketAddress()).use { s =>
      /*Smtp.rset[IO]() >>*/
      command.run(SmtpRequest(email, SmtpSocket.fromSocket[IO](s)))
    }

  def printError: PartialFunction[Throwable, Throwable] = {
    case e: pencilmail.data.Error =>
      println(s"error: ${e.show}")
      e
    case e: Throwable =>
      e.printStackTrace()
      e

  }
  extension [R](c: Smtp[IO, R])
    def runCommand(using email: Email): R = runC(c).unsafeRunSync()
    def attempt(using email: Email): Either[Throwable, R] =
      runC(c).attempt.unsafeRunSync().leftMap(printError)
