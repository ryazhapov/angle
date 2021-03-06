package io.ryazhapov.api.accounts

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.AdminRole
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.services.accounts.AdminService
import io.ryazhapov.services.accounts.AdminService.AdminService
import io.ryazhapov.services.auth.UserService
import io.ryazhapov.services.auth.UserService.UserService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class AdminApi[R <: Api.DefaultApiEnv with AdminService with UserService] extends Api[R] {

  import dsl._

  val adminRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case GET -> Root / IntVar(id) as UserWithSession(user, session) =>
      user.role match {
        case AdminRole =>

          val handleRequest = for {
            _ <- log.info(s"Getting admin $id")
            result <- AdminService.getAdmin(id)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root as UserWithSession(user, session) =>
      user.role match {
        case AdminRole =>

          val handleRequest = for {
            _ <- log.info(s"Getting all admins")
            result <- AdminService.getAllAdmins
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / "verify" / "show" as UserWithSession(user, session) =>
      user.role match {
        case AdminRole =>

          val handleRequest = for {
            _ <- log.info(s"Getting all unverified users")
            result <- UserService.getUnverifiedUsers
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case PUT -> Root / "verify" / IntVar(id) as UserWithSession(user, session) =>
      user.role match {
        case AdminRole if user.verified =>

          val handleRequest = for {
            _ <- log.info(s"Verifying user $id")
            result <- UserService.verifyUser(id)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(adminRoutes)
}