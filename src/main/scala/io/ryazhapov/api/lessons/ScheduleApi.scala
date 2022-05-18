package io.ryazhapov.api.lessons

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Role.StudentRole
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.lessons.Schedule
import io.ryazhapov.errors.ScheduleOverlapping
import io.ryazhapov.services.accounts.TeacherService
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import io.ryazhapov.services.lessons.ScheduleService
import io.ryazhapov.services.lessons.ScheduleService.ScheduleService
import org.http4s.{AuthedRoutes, HttpRoutes, QueryParamDecoder, Response}
import zio.interop.catz._
import zio.logging._
import zio.{IO, ZIO}

import java.time.ZonedDateTime
import java.util.UUID

class ScheduleApi[R <: Api.DefaultApiEnv with ScheduleService with TeacherService] extends Api[R] {

  import dsl._

  object TeacherIdParamMatcher extends QueryParamDecoderMatcher[UUID]("teacher")

  implicit val uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    QueryParamDecoder[String].map(UUID.fromString)

  case class ScheduleRequest(
    teacherId: UserId,
    startsAt: ZonedDateTime,
    endsAt: ZonedDateTime
  )

  val scheduleRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ POST -> Root / "create" as UserWithSession(user, session) =>
      user.role match {
        case StudentRole => IO(Response(Unauthorized))

        case _ =>
          val handleRequest = for {
            scheduleReq <- authReq.req.as[ScheduleRequest]
            _ <- TeacherService.getTeacher(scheduleReq.teacherId)
            _ <- log.info(s"Creating schedule for ${scheduleReq.teacherId}")
            id <- zio.random.nextUUID
            schedule = Schedule(
              id,
              scheduleReq.teacherId,
              scheduleReq.startsAt,
              scheduleReq.endsAt
            )
            _ <- ScheduleService.isOverlapping(schedule).flatMap {
              case true  => ZIO.fail(ScheduleOverlapping)
              case false => ZIO.succeed(())
            }
            result <- ScheduleService.createSchedule(schedule)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
      }

    case authReq @ PUT -> Root / "update" as UserWithSession(user, session) =>
      user.role match {
        case StudentRole => IO(Response(Unauthorized))

        case _ =>
          val handleRequest = for {
            schedule <- authReq.req.as[Schedule]
            _ <- log.info(s"Updating schedule for ${schedule.teacherId}")
            result <- ScheduleService.updateSchedule(schedule)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
      }

    case GET -> Root / UUIDVar(id) as UserWithSession(_, session) =>
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

    case DELETE -> Root / UUIDVar(id) as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Deleting schedule $id")
        result <- ScheduleService.deleteSchedule(id)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(scheduleRoutes)
}
