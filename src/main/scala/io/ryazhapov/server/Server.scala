package io.ryazhapov.server

import cats.effect.{ExitCode => CatsExitCode}
import io.ryazhapov.api.accounts.{AdminApi, TeacherApi}
import io.ryazhapov.api.auth.AuthApi
import io.ryazhapov.api.billing.{PaymentApi, ReplenishmentApi, WithdrawalApi}
import io.ryazhapov.api.lessons.{LessonApi, ScheduleApi}
import io.ryazhapov.config.Config
import io.ryazhapov.database.services.MigrationService.performMigration
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import zio.interop.catz._
import zio.magic._
import zio.{RIO, ZIO}

object Server extends Environment {

  type AppTask[A] = RIO[AppEnvironment, A]

  val httpApp = Router[AppTask](
    "/api/auth" -> new AuthApi().routes,
    "/api/teacher" -> new TeacherApi().routes,
    "/api/admin" -> new AdminApi().routes,
    "/api/schedule" -> new ScheduleApi().routes,
    "/api/lesson" -> new LessonApi().routes,
    "/api/replenishment" -> new ReplenishmentApi().routes,
    "/api/payment" -> new PaymentApi().routes,
    "/api/withdrawal" -> new WithdrawalApi().routes
  ).orNotFound

  val server = for {
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

  def start() =
    server
      .injectCustom(appEnvironment)
      .exitCode
}