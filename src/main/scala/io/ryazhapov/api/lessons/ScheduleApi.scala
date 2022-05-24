package io.ryazhapov.api.lessons

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.TeacherRole
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.lessons.ScheduleRequest
import io.ryazhapov.services.lessons.ScheduleService
import io.ryazhapov.services.lessons.ScheduleService.ScheduleService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class ScheduleApi[R <: Api.DefaultApiEnv with ScheduleService] extends Api[R] {

  import dsl._

  val scheduleRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ POST -> Root / "create" as UserWithSession(user, session) =>
      user.role match {

        case TeacherRole if user.verified =>
          val handleRequest = for {
            _ <- log.info(s"Creating schedule for ${user.email}")
            scheduleReq <- authReq.req.as[ScheduleRequest]
            result <- ScheduleService.createSchedule(user.id, scheduleReq)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case authReq @ PUT -> Root / "update" / IntVar(id) as UserWithSession(user, session) =>
      user.role match {

        case TeacherRole if user.verified =>
          val handleRequest = for {
            scheduleReq <- authReq.req.as[ScheduleRequest]
            _ <- log.info(s"Updating schedule for ${user.email}")
            result <- ScheduleService.updateSchedule(user.id, id, scheduleReq)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / IntVar(id) as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting schedule $id")
        result <- ScheduleService.getSchedule(id)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root / "all" as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting all schedules")
        result <- ScheduleService.getAllSchedules
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root :? TeacherIdParamMatcher(id) as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting schedule for teacher $id")
        result <- ScheduleService.getScheduleByTeacherId(id)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case DELETE -> Root / IntVar(id) as UserWithSession(user, session) =>
      user.role match {

        case TeacherRole if user.verified =>
          val handleRequest = for {
            _ <- log.info(s"Deleting schedule $id")
            result <- ScheduleService.deleteSchedule(user.id, id)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(scheduleRoutes)
}