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

import cats.Show

final case class CustomHeader private (name: String, value: String):
  def render: String = s"$name: $value"

object CustomHeader:
  def from(name: String, value: String): Either[Error, CustomHeader] =
    for
      _ <- Either.cond(
             isValidName(name),
             (),
             Error.PencilError(s"Invalid custom header name: '$name'")
           )
      _ <- Either.cond(
             isValidValue(value),
             (),
             Error.PencilError(s"Invalid custom header value for '$name'")
           )
    yield CustomHeader(name, value)

  def unsafe(name: String, value: String): CustomHeader =
    from(name, value).fold(throw _, identity)

  given Show[CustomHeader] = Show.show(_.render)

  private def isValidName(name: String): Boolean =
    name.nonEmpty && name.forall(ch => ch >= '!' && ch <= '~' && ch != ':')

  private def isValidValue(value: String): Boolean =
    !value.exists(ch => ch == '\r' || ch == '\n')
