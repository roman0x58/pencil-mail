package pencilmail
package data

import cats.syntax.show.*
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.ascii
import scodec.*
final case class MailboxCodec() extends Codec[Mailbox]:
  private val `<` = ByteVector("<".getBytes)
  private val `>` = ByteVector(">".getBytes)

  override def decode(bits: BitVector): Attempt[DecodeResult[Mailbox]] =
    extractEmail(bits).flatMap(
      ascii.decode(_).flatMap { case DecodeResult(box, remainder) =>
        Mailbox.fromString(box) match {
          case Right(mb)   =>
            Attempt.successful(DecodeResult(mb, remainder))
          case Left(error) =>
            Attempt.failure(Err(error.show))
        }
      }
    )

  private def extractEmail(bits: BitVector): Attempt[BitVector] = {
    val bytes = bits.toByteVector
    val from  = bytes.indexOfSlice(`<`)
    val to    = bytes.indexOfSlice(`>`)
    if from < 0 || to < 0 then Attempt.failure(Err("email does not included into '<' '>'"))
    else Attempt.successful(bits)

  }

  override def encode(mb: Mailbox): Attempt[BitVector] =
    Attempt.successful(mb.show.toBitVector)

  override def sizeBound: SizeBound = SizeBound.unknown
