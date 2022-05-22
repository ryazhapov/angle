package io.ryazhapov.api.lessons

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.accounts.Role.TeacherRole
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.lessons.Schedule
import io.ryazhapov.errors.{ScheduleOverlapping, UnauthorizedAction}
import io.ryazhapov.services.lessons.ScheduleService
import io.ryazhapov.services.lessons.ScheduleService.ScheduleService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.interop.catz._
import zio.logging._
import zio.{IO, ZIO}

import java.time.ZonedDateTime

class ScheduleApi[R <: Api.DefaultApiEnv with ScheduleService] extends Api[R] {

  import dsl._

  val scheduleRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ POST -> Root / "create" as UserWithSession(user, session) =>
      user.role match {

        case TeacherRole if user.verified =>
          val handleRequest = for {
            _ <- log.info(s"Creating schedule for ${user.id}")
            request <- authReq.req.as[ScheduleRequest]
            id = 0
            schedule = Schedule(
              id,
              user.id,
              request.startsAt,
              request.endsAt
            )
            _ <- checkOverlapping(schedule)
            result <- ScheduleService.createSchedule(schedule)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case authReq @ PUT -> Root / "update" :? ScheduleIdParamMatcher(id) as UserWithSession(user, session) =>
      user.role match {

        case TeacherRole if user.verified =>
          val handleRequest = for {
            request <- authReq.req.as[ScheduleRequest]
            _ <- log.info(s"Updating schedule for ${user.id}")
            found <- ScheduleService.getSchedule(id)
            _ <- ZIO.when(user.id == found.teacherId)(ZIO.fail(UnauthorizedAction))
            updated = Schedule(
              found.id,
              found.teacherId,
              request.startsAt,
              request.endsAt)
            _ <- checkOverlapping(updated)
            result <- ScheduleService.updateSchedule(updated)
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

    case GET -> Root as UserWithSession(_, session) =>
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
            found <- ScheduleService.getSchedule(id)
            _ <- ZIO.when(user.id == found.teacherId)(ZIO.fail(UnauthorizedAction))
            result <- ScheduleService.deleteSchedule(id)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }
  }

  def checkOverlapping(schedule: Schedule): ZIO[ScheduleService with DBTransactor, Throwable, Unit] =
    ScheduleService.isOverlapping(schedule).flatMap {
      case true  => ZIO.fail(ScheduleOverlapping)
      case false => ZIO.succeed(())
    }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(scheduleRoutes)

  case class ScheduleRequest(
    startsAt: ZonedDateTime,
    endsAt: ZonedDateTime
  )
}
