package io.ryazhapov.server

import cats.effect.{ExitCode => CatsExitCode}
import io.ryazhapov.api.accounts.{AdminApi, StudentApi, TeacherApi}
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
    "/api/v1/auth" -> new AuthApi().routes,
    "/api/v1/teacher" -> new TeacherApi().routes,
    "/api/v1/student" -> new StudentApi().routes,
    "/api/v1/admin" -> new AdminApi().routes,
    "/api/v1/schedule" -> new ScheduleApi().routes,
    "/api/v1/lesson" -> new LessonApi().routes,
    "/api/v1/replenishment" -> new ReplenishmentApi().routes,
    "/api/v1/payment" -> new PaymentApi().routes,
    "/api/v1/withdrawal" -> new WithdrawalApi().routes
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