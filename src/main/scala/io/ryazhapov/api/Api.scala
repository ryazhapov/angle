package io.ryazhapov.api

import cats.data.{Kleisli, OptionT}
import io.circe.{Decoder, Encoder}
import io.ryazhapov.config.Config
import io.ryazhapov.config.ConfigService.Configuration
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.SessionId
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.errors.{AppError, SessionCookieIsAbsent}
import io.ryazhapov.logging.LoggerService.LoggerService
import io.ryazhapov.services.accounts.AdminService.AdminService
import io.ryazhapov.services.accounts.StudentService.StudentService
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import io.ryazhapov.services.auth.UserService
import io.ryazhapov.services.auth.UserService.UserService
import org.http4s._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Cookie
import org.http4s.server.AuthMiddleware
import zio.console.Console
import zio.interop.catz._
import zio.random.Random
import zio.{IO, RIO, ZIO}

object Api {
  type DefaultApiEnv = AdminService with StudentService with TeacherService with UserService
    with DBTransactor with Random with Console with Configuration with LoggerService
}

import io.ryazhapov.api.Api._

trait Api[R <: DefaultApiEnv] extends ErrorMapping[R] {

  type ApiTask[A] = RIO[R, A]

  val dsl: Http4sDsl[ApiTask] = Http4sDsl[ApiTask]

  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[ApiTask, A] = jsonOf[ApiTask, A]

  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[ApiTask, A] = jsonEncoderOf[ApiTask, A]

  object UserIdParamMatcher extends QueryParamDecoderMatcher[String]("userId")

  def okWithCookie[A](result: A, sessionId: SessionId)
    (implicit encoder: EntityEncoder[ApiTask, A]): ZIO[R, Throwable, Response[Api.this.ApiTask]#Self] =
    for {
      config <- zio.config.getConfig[Config]
      result <- Ok(result).map(_.addCookie(ResponseCookie(
        name = "ssid",
        content = sessionId.toString,
        path = Some("/"),
        domain = Some(config.api.host)
      )))
    } yield result

  def extractCookie(request: Request[ApiTask]): IO[AppError, String] =
    ZIO.fromEither(
      request.headers.get(Cookie)
        .flatMap(_.values.toList.find(_.name == "ssid"))
        .map(_.content)
        .toRight(SessionCookieIsAbsent)
    )

  private def getUser(request: Request[ApiTask]): ApiTask[UserWithSession] = {
    for {
      sessionId <- extractCookie(request)
      session <- UserService.getSession(sessionId)
      userId = session.userId
      user <- UserService.getUser(userId)
    } yield UserWithSession(user, session)
  }

  private val authUser: Kleisli[ApiTask, Request[ApiTask], Either[String, UserWithSession]] = Kleisli { request =>
    getUser(request).foldM(
      error => ZIO.left(error.getMessage),
      result => ZIO.right(result)
    )
  }

  private val onFailure: AuthedRoutes[String, ApiTask] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))
  val authMiddleware: AuthMiddleware[ApiTask, UserWithSession] = AuthMiddleware(authUser, onFailure)

  def routes: HttpRoutes[ApiTask]
}