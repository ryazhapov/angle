package io.ryazhapov.api.lessons

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Role
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.lessons.Lesson
import io.ryazhapov.errors.{LessonOverlapping, ScheduleNotFound, UnauthorizedActionWithLesson}
import io.ryazhapov.services.accounts.StudentService.StudentService
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import io.ryazhapov.services.accounts.{StudentService, TeacherService}
import io.ryazhapov.services.lessons.LessonService.LessonService
import io.ryazhapov.services.lessons.ScheduleService.ScheduleService
import io.ryazhapov.services.lessons.{LessonService, ScheduleService}
import org.http4s.{AuthedRoutes, HttpRoutes, QueryParamDecoder, Response}
import zio.interop.catz._
import zio.logging._
import zio.{IO, ZIO}

import java.time.ZonedDateTime
import java.util.UUID

class LessonApi[R <: Api.DefaultApiEnv with LessonService with
  TeacherService with StudentService with ScheduleService] extends Api[R] {

  import dsl._

  object CompletedParamMatcher extends QueryParamDecoderMatcher[Boolean]("completed")

  implicit val uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    QueryParamDecoder[String].map(UUID.fromString)

  case class LessonRequest(
    teacherId: UserId,
    studentId: UserId,
    startsAt: ZonedDateTime,
    endsAt: ZonedDateTime
  )

  val lessonRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ POST -> Root / "create" as UserWithSession(user, session) =>
      val handleRequest = for {
        req <- authReq.req.as[LessonRequest]
        _ <- TeacherService.getTeacher(req.teacherId)
        _ <- StudentService.getStudent(req.studentId)
        _ <- log.info(s"Creating lesson for ${req.teacherId} and ${req.studentId}")
        _ <- ZIO.when(!(user.id != req.studentId || user.id != req.teacherId))(ZIO.fail(UnauthorizedActionWithLesson))
        id <- zio.random.nextUUID
        lesson = Lesson(
          id,
          req.teacherId,
          req.studentId,
          req.startsAt,
          req.endsAt,
          completed = false
        )
        _ <- LessonService.isOverlapping(lesson).flatMap {
          case true  => ZIO.fail(LessonOverlapping)
          case false => ZIO.succeed(())
        }
        _ <- ScheduleService.findScheduleForLesson(lesson.startsAt, lesson.endsAt).flatMap {
          case ::(_, _) => ZIO.succeed(())
          case Nil      => ZIO.fail(ScheduleNotFound)
        }
        result <- LessonService.createLesson(lesson)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case authReq @ PUT -> Root / "update" as UserWithSession(user, session) =>
      val handleRequest = for {
        lesson <- authReq.req.as[Lesson]
        _ <- log.info(s"Updating lesson for ${lesson.teacherId} and ${lesson.studentId}")
        _ <- ZIO.when(user.id != lesson.studentId || user.id != lesson.teacherId)(ZIO.fail(UnauthorizedActionWithLesson))
        _ <- LessonService.isOverlapping(lesson).flatMap {
          case true  => ZIO.fail(LessonOverlapping)
          case false => ZIO.succeed(())
        }
        result <- LessonService.updateLesson(lesson)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root / UUIDVar(id) as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting lesson $id")
        result <- LessonService.getLesson(id)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root / "all" as UserWithSession(user, session) =>
      user.role match {
        case Role.AdminRole =>
          val handleRequest = for {
            _ <- log.info(s"Getting all lessons")
            result <- LessonService.getAllLessons
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
        case _              => IO(Response(Unauthorized))
      }

    case GET -> Root / "all" :? CompletedParamMatcher(completed) as UserWithSession(user, session) =>
      user.role match {
        case Role.AdminRole =>

          val handleRequest = for {
            _ <- log.info(s"Getting all lessons")
            result <- LessonService.getAllLessonWithFilter(completed)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root as UserWithSession(user, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting lesson for ${user.role} ${user.id}")
        result <- LessonService.getLessonsByUser(user)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root :? CompletedParamMatcher(completed) as UserWithSession(user, session) =>
      val handleRequest = for {
        _ <- completed match {
          case true  => log.info(s"Getting completed lessons for ${user.role} ${user.id}")
          case false => log.info(s"Getting upcoming lesson for ${user.role} ${user.id}")
        }
        result <- LessonService.getLessonsByUserWithFilter(user, completed)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case DELETE -> Root / UUIDVar(id) as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Deleting lesson $id")
        result <- LessonService.deleteLesson(id)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(lessonRoutes)
}
