package io.ryazhapov.domain.accounts

import io.circe.{Decoder, Encoder}

sealed trait Role

object Role {
  def fromString(value: String): Role = Vector(AdminRole, TeacherRole, StudentRole).find(_.toString == value).get

  def toString(role: Role): String = role.toString

  implicit val decoder: Decoder[Role] = Decoder[String].emap {
    case "admin"   => Right(AdminRole)
    case "teacher" => Right(TeacherRole)
    case "student" => Right(StudentRole)
    case other     => Left(s"Invalid role: $other")
  }

  implicit val encoder: Encoder[Role] = Encoder[String].contramap {
    case AdminRole   => "admin"
    case TeacherRole => "teacher"
    case StudentRole => "student"
  }

  case object AdminRole extends Role

  case object StudentRole extends Role

  case object TeacherRole extends Role
}
