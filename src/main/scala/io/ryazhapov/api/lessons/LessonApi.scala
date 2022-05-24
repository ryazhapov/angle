package io.ryazhapov.api.lessons

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role
import io.ryazhapov.domain.accounts.Role.{AdminRole, StudentRole}
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.lessons.LessonRequest
import io.ryazhapov.services.lessons.LessonService
import io.ryazhapov.services.lessons.LessonService.LessonService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class LessonApi[R <: Api.DefaultApiEnv with LessonService] extends Api[R] {

  import dsl._

  val lessonRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ POST -> Root / "create" as UserWithSession(user, session) =>
      user.role match {

        case StudentRole if user.verified =>
          val handleRequest = for {
            lessonReq <- authReq.req.as[LessonRequest]
            _ <- log.info(s"Creating lesson for Student:${user.email} and Teacher:${lessonReq.teacherId}")
            result <- LessonService.createLesson(user.id, lessonReq)
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

    case GET -> Root / "all" / "completed" as UserWithSession(user, session) =>
      user.role match {
        case Role.AdminRole =>

          val handleRequest = for {
            _ <- log.info(s"Getting all lessons")
            result <- LessonService.getAllLessonWithFilter(true)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / "all" / "upcoming" as UserWithSession(user, session) =>
      user.role match {
        case Role.AdminRole =>

          val handleRequest = for {
            _ <- log.info(s"Getting all lessons")
            result <- LessonService.getAllLessonWithFilter(false)
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
            _ <- log.info(s"Getting lesson for ${user.role} ${user.email}")
            result <- LessonService.getLessonsByUser(user)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
      }


    case GET -> Root / "completed" as UserWithSession(user, session) =>
      user.role match {
        case AdminRole => IO(Response(MethodNotAllowed))

        case _ =>
          val handleRequest = for {
            _ <- log.info(s"Getting completed lessons for ${user.email}")
            result <- LessonService.getLessonsByUserWithFilter(user, true)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
      }

    case GET -> Root / "upcoming" as UserWithSession(user, session) =>
      user.role match {
        case AdminRole => IO(Response(MethodNotAllowed))

        case _ =>
          val handleRequest = for {
            _ <- log.info(s"Getting upcoming lessons for ${user.role} ${user.email}")
            result <- LessonService.getLessonsByUserWithFilter(user, false)
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
            result <- LessonService.deleteLesson(user.id, id)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
      }
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(lessonRoutes)
}