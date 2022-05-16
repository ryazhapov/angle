package io.ryazhapov.api.auth

import cats.implicits._
import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.Password
import io.ryazhapov.domain.accounts.Role.{AdminRole, StudentRole, TeacherRole}
import io.ryazhapov.domain.accounts.{Admin, Role, Student, Teacher}
import io.ryazhapov.domain.auth.{User, UserWithSession}
import io.ryazhapov.errors.UserAlreadyExists
import io.ryazhapov.services.accounts.{AdminService, StudentService, TeacherService}
import io.ryazhapov.services.auth.UserService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.interop.catz._
import zio.logging._
import zio.{IO, ZIO}

class AuthApi[R <: Api.DefaultApiEnv] extends Api[R] {

  import dsl._

  case class SignInUser(email: String, password: Password)

  case class SignUpUser(email: String, password: Password, role: Role)

  val standardRoutes: HttpRoutes[ApiTask] = HttpRoutes.of[ApiTask] {

    case req @ POST -> Root / "sign_up" =>
      val requestHandler = for {
        _ <- log.info("Sign up attempt")
        user <- req.as[SignUpUser]
        id <- zio.random.nextUUID
        _ <- UserService.userExists(user.email).flatMap {
          case false => ZIO.succeed(())
          case true  => ZIO.fail(UserAlreadyExists)
        }
        _ <- UserService.createUser(User(id,
          user.email,
          user.role,
          isVerified = if (user.role == AdminRole) true else false),
          user.password)
        _ <- user.role match {
          case AdminRole   => AdminService.createAdmin(Admin(userId = id))
          case TeacherRole => TeacherService.createTeacher(Teacher(userId = id))
          case StudentRole => StudentService.createStudent(Student(userId = id))
        }
        sessionId <- UserService.addSession(id)
        _ <- log.info(s"User with email=${user.email} signed up successfully")
      } yield (id, sessionId)

      requestHandler.foldM(
        throwableToHttpCode,
        { case (userId, sessionId) => okWithCookie(userId, sessionId) }
      )

    case req @ POST -> Root / "sign_in" =>
      val requestHandler = for {
        apiUser <- req.as[SignInUser]
        _ <- log.info(s"User ${apiUser.email} try to login")
        user <- UserService.validateUser(apiUser.email, apiUser.password)
        sessionId <- UserService.addSession(user.id)
        _ <- log.info(s"User ${apiUser.email} logged in successfully")
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
    case GET -> Root / "users" as UserWithSession(user, session) =>
      user.role match {
        case AdminRole => log.info("Get users") *>
          UserService.getAllUsers.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id))
        case _         => IO(Response(Unauthorized))
      }

    case GET -> Root / "user" / UUIDVar(id) as UserWithSession(user, session) =>
      user.role match {
        case AdminRole => log.info("Get user") *>
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
        ) <* log.info(s"User ${user.email} signed out successfully")
  }

  override def routes: HttpRoutes[ApiTask] =
    standardRoutes <+> authMiddleware(authedRoutes)
}