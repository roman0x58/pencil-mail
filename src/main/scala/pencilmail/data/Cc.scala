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
import cats.data.NonEmptyList
import cats.kernel.Semigroup
import cats.syntax.show.*
object CcType:

  opaque type Cc = NonEmptyList[Mailbox]
  object Cc:

    def apply(boxes: Mailbox*): Cc =
      NonEmptyList.fromListUnsafe(boxes.toList)

    extension (self: Cc)
      def +(other: Cc): Cc                 = self ::: other
      def mailboxes: NonEmptyList[Mailbox] = self
      def toList: List[Mailbox]            = self.toList

    given Show[Cc] =
      Show.show(cc => cc.map(_.show).toList.mkString(","))

    given Semigroup[Cc] =
      Semigroup.instance[Cc]((a, b) => a + b)
