package io.ryazhapov.domain.accounts

import io.circe.{Decoder, Encoder}
import io.getquill.MappedEncoding

sealed trait Level

object Level {
  def fromString(value: String): Level = Vector(
    Beginner,
    Elementary,
    Intermediate,
    UpperIntermediate,
    Advanced,
    Proficiency
  ).find(_.toString == value).get

  def toString(level: Level): String = level.toString

  implicit val encodeLevel: MappedEncoding[Level, String] = MappedEncoding[Level, String](_.toString)
  implicit val decodeLevel: MappedEncoding[String, Level] = MappedEncoding[String, Level](Level.fromString)

  implicit val decoder: Decoder[Level] = Decoder[String].emap {
    case "beginner"           => Right(Beginner)
    case "elementary"         => Right(Elementary)
    case "intermediate"       => Right(Intermediate)
    case "upper_intermediate" => Right(UpperIntermediate)
    case "advanced"           => Right(Advanced)
    case "proficiency"        => Right(Proficiency)
    case other                => Left(s"Invalid level: $other")
  }

  implicit val encoder: Encoder[Level] = Encoder[String].contramap {
    case Beginner          => "beginner"
    case Elementary        => "elementary"
    case Intermediate      => "intermediate"
    case UpperIntermediate => "upper_intermediate"
    case Advanced          => "advanced"
    case Proficiency       => "proficiency"
  }

  case object Beginner extends Level

  case object Elementary extends Level

  case object Intermediate extends Level

  case object UpperIntermediate extends Level

  case object Advanced extends Level

  case object Proficiency extends Level
}

