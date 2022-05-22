package io.ryazhapov.domain.accounts

import io.circe.{Decoder, Encoder}
import io.getquill.MappedEncoding

sealed trait Role

object Role {
  def fromString(value: String): Role = Vector(AdminRole, TeacherRole, StudentRole).find(_.toString == value).get

  def toString(role: Role): String = role.toString

  implicit val encodeRole: MappedEncoding[Role, String] = MappedEncoding[Role, String](_.toString)
  implicit val decodeRole: MappedEncoding[String, Role] = MappedEncoding[String, Role](Role.fromString)

  implicit val roleDecoder: Decoder[Role] = Decoder[String].emap {
    case "admin"   => Right(AdminRole)
    case "teacher" => Right(TeacherRole)
    case "student" => Right(StudentRole)
    case other     => Left(s"Invalid role: $other")
  }

  implicit val roleEncoder: Encoder[Role] = Encoder[String].contramap {
    case AdminRole   => "admin"
    case TeacherRole => "teacher"
    case StudentRole => "student"
  }

  case object AdminRole extends Role

  case object StudentRole extends Role

  case object TeacherRole extends Role
}
