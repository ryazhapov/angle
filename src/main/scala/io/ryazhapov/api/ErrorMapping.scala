package io.ryazhapov.api

import io.ryazhapov.errors._
import org.http4s.Response
import zio.interop.catz._
import zio.logging._

trait ErrorMapping[R <: Api.DefaultApiEnv] {
  this: Api[R] =>

  import dsl._

  def throwableToHttpCode(e: Throwable): ApiTask[Response[ApiTask]] = {
    val resultCode = e match {
      case SessionCookieIsAbsent => BadRequest(e.getMessage)
      case IncorrectUserPassword => BadRequest(e.getMessage)

      case UserAlreadyExists    => Conflict(e.getMessage)
      case TeacherAlreadyExists => Conflict(e.getMessage)
      case StudentAlreadyExists => Conflict(e.getMessage)

      case ScheduleOverlapping   => Conflict(e.getMessage)
      case ScheduleAlreadyExists => Conflict(e.getMessage)
      case InvalidScheduleTime   => Conflict(e.getMessage)

      case UserNotExist(_)  => NotFound(e.getMessage)
      case UserNotFound     => NotFound(e.getMessage)
      case TeacherNotFound  => NotFound(e.getMessage)
      case StudentNotFound  => NotFound(e.getMessage)
      case ScheduleNotFound => NotFound(e.getMessage)
      case SessionNotFound  => NotFound(e.getMessage)

      case _ => InternalServerError(e.getMessage)
    }
    log.error(e.getMessage) *> resultCode
  }
}
