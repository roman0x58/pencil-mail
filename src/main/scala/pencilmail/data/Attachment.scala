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
package data

import cats.effect.Sync
import cats.implicits.*

import java.net.{URI, URL}
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}

object AttachmentType:

  opaque type Attachment = Path
  object Attachment:

    def apply(path: Path): Attachment = path

    def fromString[F[_]](file: String)(using Sync[F]): F[Attachment] = Files
      .pathFrom[F](file)
      .handleErrorWith { _ =>
        Files.pathFromClassLoader[F](file)
      }

    extension (self: Attachment)
      def file: Path      = self
      def encoded: String = {
        val utf8Bytes = self.getFileName.toString.getBytes(StandardCharsets.UTF_8)
        val encoded   = utf8Bytes.map("%02X".format(_)).mkString("%")
        s"UTF-8''$encoded"
      }
