package io.ryazhapov.api.accounts

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.{AdminRole, TeacherRole}
import io.ryazhapov.domain.accounts.Teacher
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.services.accounts.TeacherService
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class TeacherApi[R <: Api.DefaultApiEnv with TeacherService] extends Api[R] {

  import dsl._

  val teacherRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ PUT -> Root / "update" / UUIDVar(id) as UserWithSession(user, session) =>
      user.role match {
        case AdminRole | TeacherRole if id == user.id =>
          val handleRequest = for {
            _ <- log.info(s"Updating teacher $id")
            request <- authReq.req.as[Teacher]
            found <- TeacherService.getTeacher(id)
            updated = request.copy(userId = found.userId)
            result <- TeacherService.updateTeacher(updated)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / UUIDVar(id) as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting teacher $id")
        result <- TeacherService.getTeacher(id)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting all teachers")
        result <- TeacherService.getAllTeachers
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(teacherRoutes)
}
