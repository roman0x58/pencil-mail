/*
 * Copyright 2020 Kaspar Minosiants
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pencilmail
package protocol

import cats.Show
import pencilmail.data.*
import pencilmail.protocol.Command.*
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.*
import scodec.{Attempt, Codec, DecodeResult, SizeBound}

final case class CommandCodec() extends Codec[Command] {

  private val END                          = Command.end.toBitVector
  private val SPACE                        = ByteVector(" ".getBytes).toBitVector

  override def decode(bits: BitVector): Attempt[DecodeResult[Command]] =
    limitedSizeBits(4 * 8, ascii).decode(bits).flatMap { case DecodeResult(cmd, rest) =>
      cmd match {
        case "EHLO" =>
          ascii.decode(extractText(rest)).map { case DecodeResult(domain, _) =>
            DecodeResult(Ehlo(domain), BitVector.empty)
          }
        case "MAIL" =>
          summon[Codec[Mailbox]].decode(rest.drop(6 * 8)).map { case DecodeResult(email, _) =>
            DecodeResult(Mail(email), BitVector.empty)
          }
        case "RCPT" =>
          summon[Codec[Mailbox]].decode(rest.drop(4 * 8)).map { case DecodeResult(email, _) =>
            DecodeResult(Rcpt(email), BitVector.empty)
          }
        case "DATA" => Attempt.successful(DecodeResult(Data, BitVector.empty))
        case "QUIT" => Attempt.successful(DecodeResult(Quit, BitVector.empty))
        case "RSET" => Attempt.successful(DecodeResult(Rset, BitVector.empty))
        case "NOOP" => Attempt.successful(DecodeResult(Noop, BitVector.empty))
        case "VRFY" =>
          ascii.decode(extractText(rest)).map { case DecodeResult(txt, _) =>
            DecodeResult(Vrfy(txt), BitVector.empty)
          }
        case "AUTH" =>
          Attempt.successful(DecodeResult(AuthLogin, BitVector.empty))
        case "STAR" =>
          Attempt.successful(DecodeResult(StartTls, BitVector.empty))
        case _      =>
          ascii.decode(bits).map { case DecodeResult(txt, _) =>
            DecodeResult(Text(txt), BitVector.empty)
          }
      }
    }

  private def extractText(bits: BitVector) =
    bits.drop(SPACE.size).dropRight(END.size)

  override def encode(v: Command): Attempt[BitVector] =
    Attempt.successful(summon[Show[Command]].show(v).toBitVector)

  override def sizeBound: SizeBound = SizeBound.unknown

}
