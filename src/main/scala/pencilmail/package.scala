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

import scodec.bits.{BitVector, ByteVector}

import java.nio.charset.StandardCharsets

val CRLF: ByteVector = ByteVector("\r\n".getBytes)

trait StrOps:
  extension (str: String)
    def toBase64: String         = toBitVector.toBase64
    def toBitVector: BitVector   = BitVector(str.getBytes(StandardCharsets.UTF_8))
    def toByteVector: ByteVector = toBitVector.bytes

given strOps: StrOps()

type To = pencilmail.data.ToType.To
val To = pencilmail.data.ToType.To
type From = pencilmail.data.FromType.From
val From = pencilmail.data.FromType.From
type Cc = pencilmail.data.CcType.Cc
val Cc = pencilmail.data.CcType.Cc
type Bcc = pencilmail.data.BccType.Bcc
val Bcc = pencilmail.data.BccType.Bcc
type Subject = pencilmail.data.SubjectType.Subject
val Subject = pencilmail.data.SubjectType.Subject
type Attachment = pencilmail.data.AttachmentType.Attachment
val Attachment = pencilmail.data.AttachmentType.Attachment
type Host = pencilmail.data.HostType.Host
val Host = pencilmail.data.HostType.Host
type Boundary = pencilmail.data.BoundaryType.Boundary
val Boundary = pencilmail.data.BoundaryType.Boundary
type Username = pencilmail.data.UsernameType.Username
val Username = pencilmail.data.UsernameType.Username
type Password = pencilmail.data.PasswordType.Password
val Password = pencilmail.data.PasswordType.Password
type Name = pencilmail.data.NameType.Name
val Name = pencilmail.data.NameType.Name
