package pencilmail

import pencilmail.data.*

import java.time.Instant
import java.util.UUID
final case class SmtpRequest[F[_]](
    email: Email,
    socket: SmtpSocket[F],
    host: Host = Host.local(),
    timestamp: Instant = Instant.now(),
    uuid: () => String = () => UUID.randomUUID().toString
)
