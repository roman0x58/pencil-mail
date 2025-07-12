package pencilmail.lib

import com.comcast.ip4s.{Hostname, Port, SocketAddress, host}
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{BindMode, GenericContainer}
import org.testcontainers.utility.DockerImageName
import pencilmail.data.Credentials
import pencilmail.*

case class ContainerConfiguration(smtpMode: SmtpMode = SmtpMode.Plain)

trait MailServerContainer:
  def start(): Unit
  def stop(): Unit
  def smtpPort: Int
  def httpPort: Int
  def socketAddress(): SocketAddress[Hostname] = SocketAddress(host"localhost", Port.fromInt(smtpPort).get)

  def credentials: Credentials
object MailServerContainer:
  def mk(configuration: ContainerConfiguration = ContainerConfiguration()): MailServerContainer =
    val username = Username("pencil")
    val password = Password("pencil1234")
    val smtp = 1025
    val http = 8025

    val container = GenericContainer(DockerImageName.parse("axllent/mailpit"))
    container.withClasspathResourceMapping("certs", "/data", BindMode.READ_ONLY)
    container.addExposedPorts(smtp, http)
    
    def applyTLS = {
      container.addEnv("MP_SMTP_AUTH_FILE", "/data/pass.txt")
      container.addEnv("MP_SMTP_TLS_CERT", "/data/certificate.crt")
      container.addEnv("MP_SMTP_TLS_KEY", "/data/keyfile.key")
    }
    configuration.smtpMode match {
      case SmtpMode.Plain =>
        container.addEnv("MP_SMTP_AUTH_ALLOW_INSECURE", "true")
      case SmtpMode.StartTLS  =>
        container.addEnv("MP_SMTP_AUTH_ALLOW_INSECURE", "true")
        applyTLS
      case SmtpMode.TLS =>
        container.addEnv("MP_SMTP_REQUIRE_TLS", "true")
        applyTLS
    }

    new MailServerContainer:
      override def start(): Unit =
        container.start()
        container.waitingFor(Wait.forListeningPort())
        println(s"http port:$httpPort")

      override def stop(): Unit = container.stop()

      override def credentials: Credentials = Credentials(username, password)

      override def httpPort: Int = container.getMappedPort(http)

      override def smtpPort: Int = container.getMappedPort(smtp)
