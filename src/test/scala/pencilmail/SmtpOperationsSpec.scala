package pencilmail

import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.syntax.option.*
import cats.syntax.show.*
import pencilmail.lib.{DataSamples, Fixtures, SmtpBaseSpec}
import pencilmail.protocol.Replies
import scodec.codecs

import java.nio.file.Path

class SmtpOperationsSpec extends SmtpBaseSpec {
  sequential

  val requests = new SmtpOperations[IO](credentials.some)(using logger)
  
  private def snapshot(log: String, path: String): Path = {
    val snap = java.nio.file.Files.writeString(Files.pathFromClassLoader[IO](path).unsafeRunSync(), log)
    logger.debug(s"Snapshot log written to $snap")
    snap
  }

  def exec(command: SMTPOperation[IO], path: Option[String] = None, makeSnapshot: Boolean = false) = {
    val response = runTestCommandWithSocket(command, Fixtures.mimeEmail, codecs.ascii)
    path collect {
      case path if makeSnapshot =>
        val log = response.map(_.response.map(v => s"""\nC:${v.cmd.show}S:${v.reply.show}""").mkString("")).toOption.getOrElse("Invalid response")
        snapshot(log, path)
    }
    response.map(_.reply) must beRight(DataSamples.`221 Buy`)
  }

  "Smtp client" should {
    "send an email using Start TLS mode" in {
      exec(requests.startTlsOperation, "snapshots/startTls.txt".some)
    }
    "send an email using TLS mode" in {
      exec(requests.startTlsOperation, "snapshots/tls.txt".some)
    }
    "send an email using Plain mode" in {
      exec(requests.plainOperation, "snapshots/plain.txt".some)
    }
  }
}
