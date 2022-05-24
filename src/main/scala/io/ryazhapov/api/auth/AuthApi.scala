package io.ryazhapov.api.auth

import cats.implicits._
import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.AdminRole
import io.ryazhapov.domain.auth.{SignInRequest, SignUpRequest, UserWithSession}
import io.ryazhapov.services.auth.UserService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class AuthApi[R <: Api.DefaultApiEnv] extends Api[R] {

  import dsl._

  val standardRoutes: HttpRoutes[ApiTask] = HttpRoutes.of[ApiTask] {

    case req @ POST -> Root / "sign_up" =>
      val requestHandler = for {
        reqUser <- req.as[SignUpRequest]
        _ <- log.info(s"${reqUser.email} -- registration")
        userId <- UserService.createUser(reqUser)
        sessionId <- UserService.addSession(userId)
        _ <- log.info(s"${reqUser.email} -- successful")
      } yield (userId, sessionId)

      requestHandler.foldM(
        throwableToHttpCode,
        { case (userId, sessionId) => okWithCookie(userId, sessionId) }
      )

    case req @ POST -> Root / "sign_in" =>
      val requestHandler = for {
        reqUser <- req.as[SignInRequest]
        _ <- log.info(s"${reqUser.email} -- login")
        user <- UserService.validateUser(reqUser.email, reqUser.password)
        sessionId <- UserService.addSession(user.id)
        _ <- log.info(s"${reqUser.email} -- success")
      } yield (user.id, sessionId)

      requestHandler.foldM(
        throwableToHttpCode,
        { case (userId, sessionId) => okWithCookie(userId, sessionId) }
      )

    case req @ GET -> Root / "check_session" =>
      val requestHandler = for {
        _ <- log.info("Check session")
        sessionId <- extractCookie(req)
        session <- UserService.getSession(sessionId)
      } yield session

      requestHandler.foldM(
        throwableToHttpCode,
        session => okWithCookie(session.userId, session.id)
      )
  }
  val authedRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {
    case GET -> Root / "user" / "all" as UserWithSession(user, session) =>
      user.role match {
        case AdminRole => log.info("Get all users") *>
          UserService.getAllUsers.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id))

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / "user" / IntVar(id) as UserWithSession(user, session) =>
      user.role match {
        case AdminRole => log.info(s"Get user with $id") *>
          UserService.getUser(id).foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id))

        case _ => IO(Response(Unauthorized))
      }

    case DELETE -> Root / "sign_out" as UserWithSession(user, session) =>
      log.info("Sign out") *>
        UserService.deleteSession(session.id).foldM(
          throwableToHttpCode,
          result => Ok(result)
        ) <* log.info(s"User signed out: ${user.email}")
  }

  override def routes: HttpRoutes[ApiTask] =
    standardRoutes <+> authMiddleware(authedRoutes)
}