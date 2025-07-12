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

import cats.effect.Sync
import pencilmail.data.*
import pencilmail.protocol.*

import java.net.URLConnection
import java.nio.file.Path

object ContentTypeFinder:
  def findType[F[_]: Sync](path: Path): F[ContentType] =
    Sync[F]
      .defer {
        if (path.toFile.exists()) {
          val guess = Option(URLConnection.guessContentTypeFromName(path.getFileName.toString))
          Sync[F].pure(guess.flatMap(ContentType.findType).getOrElse(ContentType.`application/octet-stream`))
        } else Sync[F].raiseError(Error.ResourceNotFound(path.toString))
      }
