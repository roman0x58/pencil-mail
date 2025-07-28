[![Maven Central](https://maven-badges.sml.io/maven-central/io.github.roman0x58/pencil-mail/badge.svg)](https://maven-badges.sml.io/maven-central/io.github.roman0x58/pencil-mail)
# PencilMail

Fork of https://github.com/minosiants/pencil with added AWS SES support, various fixes, improved tests, various enhancements, and removal of the Apache Tika dependency.

### Overview

`PencilMail` is a simple SMTP client. Its main goal is to send emails in the simplest possible way.  
It is built on top of [cats](https://typelevel.org/cats/), [cats-effect](https://typelevel.org/cats-effect/), [fs2](https://fs2.io/), and [scodec](http://scodec.org/).

`PencilMail` supports:
* Text emails (ASCII)
* MIME emails
* TLS
* Authentication

### Specifications

* [RFC 5321 - Simple Mail Transfer Protocol](https://tools.ietf.org/html/rfc5321)
* Multipurpose Internet Mail Extensions
    * [RFC 2045 - Format of Internet Message Bodies](https://tools.ietf.org/html/rfc2045)
    * [RFC 2046 - Media Types](https://tools.ietf.org/html/rfc2046)
    * [RFC 2047 - Message Header Extensions for Non-ASCII Text](https://tools.ietf.org/html/rfc2047)
    * [RFC 2049 - Conformance Criteria and Examples](https://tools.ietf.org/html/rfc2049)
    * [RFC 4288 - Media Type Specifications and Registration Procedures](https://tools.ietf.org/html/rfc4288)
    * [RFC 1521 - Mechanisms for Specifying and Describing the Format of Internet Message Bodies](https://tools.ietf.org/html/rfc1521)
    * [RFC 1522 - Message Header Extensions for Non-ASCII Text](https://tools.ietf.org/html/rfc1522)
    * [RFC 4954 - SMTP Service Extension for Authentication](https://tools.ietf.org/html/rfc4954)

### Usage

Add dependency to your `build.sbt`

#### For Scala 3

```scala
libraryDependencies += "io.github.roman0x58" %% "pencilmail" % "3.0.5"

```

### Examples

#### Create text email

```scala
val email = Email.text(
      from"user name <user1@mydomain.tld>",
      to"user1@example.com",
      subject"first email",
      Body.Ascii("hello")
)

```

#### Create MIME email with attachment

```scala
val email = Email.mime(
     from"user1@mydomain.tld",
     to"user1@example.com",
     subject"привет",
     Body.Utf8("hi there")
) + attachment"path/to/file"

```

#### Create MIME email with alternative bodies

```scala
val email = Email.mime(
     from"user1@mydomain.tld",
     to"user1@example.com",
     subject"привет",
     Body.Alternative(List(Body.Utf8("hi there3"), Body.Ascii("hi there2")))
)

```

#### Send email example

```scala
object Main extends IOApp {

  val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val credentials = Credentials(
      Username("user1@mydomain.tld"),
      Password("password")
    )
    val action = for {
      tls <- Network[IO].tlsContext.system
      client = Client[IO](
        address = SocketAddress(host"localhost", port"25"),
        mode = SmtpMode.Plain,
        Some(credentials)
      )(tls, logger)
      response <- client.send(email)
    } yield response

    action.attempt.flatMap {
      case Right(replies) =>
        logger.debug(replies)
      case Left(error) =>
        error match {
          case e: Error => logger.debug(e.toString)
          case e: Throwable => logger.debug(e.getMessage)
        }
    }.unsafeRunSync()
  }
}

```

## Development

### Creating test truststore

```bash
keytool -import -trustcacerts -keystore test-truststore.jks -storepass changeit -alias mailpit -file certificate.crt

```

### Running AWS SES integration tests

```bash
sbt -Daws.username=username -Daws.password=password -Daws.from=from -Daws.to=to

```

### Docker Mailserver

For testing purposes, [Docker Mailserver](https://github.com/jeboehm/docker-mailserver) can be used.