package io.ryazhapov.server

import cats.data.Kleisli
import cats.effect.{ExitCode => CatsExitCode}
import io.ryazhapov.api.accounts.{AdminApi, TeacherApi}
import io.ryazhapov.api.auth.AuthApi
import io.ryazhapov.api.lessons.ScheduleApi
import io.ryazhapov.config.Config
import io.ryazhapov.database.services.MigrationService.performMigration
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{Request, Response}
import zio.console.Console
import zio.interop.catz._
import zio.{ExitCode, RIO, URIO, ZEnv, ZIO}

object Server extends Environment {

  type AppTask[A] = RIO[AppEnvironment, A]

  val httpApp: Kleisli[AppTask, Request[AppTask], Response[AppTask]] = Router[AppTask](
    "/api/auth" -> new AuthApi().routes,
    "/api/teacher" -> new TeacherApi().routes,
    "/api/admin" -> new AdminApi().routes,
    "/api/schedule" -> new ScheduleApi().routes
  ).orNotFound

  val server: ZIO[AppEnvironment, Throwable, Unit] = for {
    config <- zio.config.getConfig[Config]
    _ <- performMigration
    _ <- ZIO.runtime[AppEnvironment].flatMap { implicit runtime =>
      val ec = runtime.platform.executor.asEC
      BlazeServerBuilder[AppTask](ec)
        .bindHttp(config.api.port, config.api.host)
        .withHttpApp(httpApp)
        .serve
        .compile[AppTask, AppTask, CatsExitCode]
        .drain
    }
  } yield ()

  def start(): URIO[ZEnv with Console, ExitCode] =
    server
      .provideSomeLayer[ZEnv](appEnvironment)
      .exitCode
}
