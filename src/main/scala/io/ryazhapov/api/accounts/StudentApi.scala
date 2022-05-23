package io.ryazhapov.api.accounts

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.StudentRole
import io.ryazhapov.domain.accounts.StudentUpdateRequest
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.services.accounts.StudentService
import io.ryazhapov.services.accounts.StudentService.StudentService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class StudentApi[R <: Api.DefaultApiEnv with StudentService] extends Api[R] {

  import dsl._

  val studentRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ PUT -> Root / "update" / IntVar(id) as UserWithSession(user, session) =>
      user.role match {
        case StudentRole if id == user.id && user.verified =>
          val handleRequest = for {
            _ <- log.info(s"Updating student ${user.email}")
            request <- authReq.req.as[StudentUpdateRequest]
            result <- StudentService.updateStudent(user.id, request)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / IntVar(id) as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting student $id")
        result <- StudentService.getStudent(id)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting all students")
        result <- StudentService.getAllStudents
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(studentRoutes)
}