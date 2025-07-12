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

package pencilmail.data

import cats.*

// import cats.effect.IO

import scala.util.control.NoStackTrace

sealed trait Error1 extends NoStackTrace with Product with Serializable

enum Error extends NoStackTrace with Product with Serializable:
  case AuthError(msg: String)
  case SmtpError(msg: String)
  case TimeoutError(msg: String)
  case PencilError(msg: String)
  case InvalidMailBox(msg: String)
  case UnableCloseResource(msg: String)
  case ResourceNotFound(msg: String)

object Error:
  given Show[Error] = {
    case SmtpError(msg)           => s"Smtp error: $msg "
    case PencilError(msg)         => s"Pencil error: $msg "
    case TimeoutError(msg)        => s"Timeout error: $msg"
    case AuthError(msg)           => s"Auth error: $msg"
    case InvalidMailBox(msg)      => s"Invalid maildbox: $msg"
    case UnableCloseResource(msg) => s"Unable close resource: $msg"
    case ResourceNotFound(msg)    => s"Resource not found: $msg"
  }

  def smtpError[F[_], A](msg: String)(using F: ApplicativeThrow[F]): F[A] =
    F.raiseError[A](SmtpError(msg))

  def timeout[F[_], A](msg: String)(using F: ApplicativeThrow[F]): F[A] =
    F.raiseError[A](TimeoutError(msg))

  def authError[F[_], A](msg: String)(using F: ApplicativeThrow[F]): F[A] =
    F.raiseError[A](AuthError(msg))

  def unableCloseResource[F[_], A](
      msg: String
  )(using F: ApplicativeThrow[F]): F[A] =
    F.raiseError(UnableCloseResource(msg))

  def resourceNotFound[F[_], A](
      msg: String
  )(using F: ApplicativeThrow[F]): F[A] =
    F.raiseError(ResourceNotFound(msg))

