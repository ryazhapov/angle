package io.ryazhapov.api.lessons

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Role
import io.ryazhapov.domain.accounts.Role.{AdminRole, StudentRole}
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.lessons.Lesson
import io.ryazhapov.errors.{DeletingCompletedLesson, LessonOverlapping, NotEnoughMoney, ScheduleNotFound, UnauthorizedAction}
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import io.ryazhapov.services.accounts.{StudentService, TeacherService}
import io.ryazhapov.services.lessons.LessonService.LessonService
import io.ryazhapov.services.lessons.ScheduleService.ScheduleService
import io.ryazhapov.services.lessons.{LessonService, ScheduleService}
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.interop.catz._
import zio.logging._
import zio.{IO, ZIO}

import java.time.ZonedDateTime

class LessonApi[R <: Api.DefaultApiEnv with LessonService with
  TeacherService with ScheduleService] extends Api[R] {

  import dsl._

  val lessonRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ POST -> Root / "create" as UserWithSession(user, session) =>
      user.role match {

        case StudentRole if user.verified =>
          val handleRequest = for {
            request <- authReq.req.as[LessonRequest]
            foundStudent <- StudentService.getStudent(user.id)
            foundTeacher <- TeacherService.getTeacher(request.teacherId)
            _ <- log.info(s"Creating lesson for Student:${user.id} and Teacher:${foundTeacher.userId}")
            _ <- ZIO.when(foundStudent.balance >= foundTeacher.rate)(ZIO.fail(NotEnoughMoney))
            id = 0
            lesson = Lesson(
              id,
              foundTeacher.userId,
              user.id,
              request.startsAt,
              request.endsAt,
              completed = false
            )
            _ <- checkOverlapping(lesson)
            _ <- ScheduleService.findScheduleForLesson(lesson.startsAt, lesson.endsAt).flatMap {
              case ::(_, _) => ZIO.succeed(())
              case Nil      => ZIO.fail(ScheduleNotFound)
            }
            updatedStudent = foundStudent.copy(
              balance = foundStudent.balance - foundTeacher.rate,
              reserved = foundStudent.reserved + foundTeacher.rate
            )
            result <- StudentService.updateStudent(updatedStudent) *> LessonService.createLesson(lesson)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / IntVar(id) as UserWithSession(_, session) =>
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

        case _ => IO(Response(Unauthorized))
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
      user.role match {
        case AdminRole => IO(Response(MethodNotAllowed))

        case _ =>
          val handleRequest = for {
            _ <- log.info(s"Getting lesson for ${user.role} ${user.id}")
            result <- LessonService.getLessonsByUser(user)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
      }


    case GET -> Root :? CompletedParamMatcher(completed) as UserWithSession(user, session) =>
      user.role match {
        case AdminRole => IO(Response(MethodNotAllowed))

        case _ =>
          val handleRequest = for {
            _ <- if (completed) {
              log.info(s"Getting completed lessons for ${user.role} ${user.id}")
            } else {
              log.info(s"Getting upcoming lesson for ${user.role} ${user.id}")
            }
            result <- LessonService.getLessonsByUserWithFilter(user, completed)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
      }

    case DELETE -> Root / IntVar(id) as UserWithSession(user, session) =>
      user.role match {
        case AdminRole => IO(Response(MethodNotAllowed))

        case _ if user.verified =>
          val handleRequest = for {
            _ <- log.info(s"Deleting lesson $id")
            foundLesson <- LessonService.getLesson(id)
            _ <- ZIO.when(!foundLesson.completed)(ZIO.fail(DeletingCompletedLesson))
            _ <- ZIO.when(!(user.id != foundLesson.teacherId || user.id != foundLesson.studentId)
            )(ZIO.fail(UnauthorizedAction))
            foundStudent <- StudentService.getStudent(foundLesson.studentId)
            foundTeacher <- TeacherService.getTeacher(foundLesson.teacherId)
            updatedStudent = foundStudent.copy(
              balance = foundStudent.balance + foundTeacher.rate,
              reserved = foundStudent.reserved - foundTeacher.rate
            )
            result <- StudentService.updateStudent(updatedStudent) *> LessonService.deleteLesson(id)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
      }
  }

  def checkOverlapping(lesson: Lesson): ZIO[LessonService with DBTransactor, Throwable, Unit] =
    LessonService.isOverlapping(lesson).flatMap {
      case true  => ZIO.fail(LessonOverlapping)
      case false => ZIO.succeed(())
    }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(lessonRoutes)

  case class LessonRequest(
    teacherId: UserId,
    startsAt: ZonedDateTime,
    endsAt: ZonedDateTime
  )
}
