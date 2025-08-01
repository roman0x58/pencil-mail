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

object UsernameType:
  opaque type Username = String
  object Username:
    def apply(username: String): Username = if (Option(username).isEmpty)
      throw Error.PencilError("Username can't be null")
    else username

    given Show[Username] = Show.show[Username](identity)
object PasswordType:
  opaque type Password = String

  object Password:
    def apply(password: String): Password = if (Option(password).isEmpty)
      throw Error.PencilError("Password can't be null")
    else password

    given Show[Password] = Show.show[Password](identity)

final case class Credentials(username: UsernameType.Username, password: PasswordType.Password)
    extends Product
    with Serializable
