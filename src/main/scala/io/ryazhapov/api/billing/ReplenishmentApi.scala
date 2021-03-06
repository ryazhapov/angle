package io.ryazhapov.api.billing

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.{AdminRole, StudentRole}
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.billing.ReplenishmentRequest
import io.ryazhapov.services.billing.ReplenishmentService
import io.ryazhapov.services.billing.ReplenishmentService.ReplenishmentService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class ReplenishmentApi[R <: Api.DefaultApiEnv with ReplenishmentService] extends Api[R] {

  import dsl._

  val replenishmentRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ PUT -> Root / "create" as UserWithSession(user, session) =>
      user.role match {
        case StudentRole =>
          val handleRequest = for {
            _ <- log.info(s"Creating replenishment for ${user.email}")
            replenishReq <- authReq.req.as[ReplenishmentRequest]
            result <- ReplenishmentService.createReplenishment(user.id, replenishReq)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root as UserWithSession(user, session) =>
      user.role match {
        case AdminRole if user.verified =>
          val handleRequest = for {
            _ <- log.info(s"Getting all replenishments")
            result <- ReplenishmentService.getAllReplenishments
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
        case StudentRole                =>
          val handleRequest = for {
            _ <- log.info(s"Getting all replenishments of student ${user.email}")
            result <- ReplenishmentService.getStudentsReplenishment(user.id)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(replenishmentRoutes)
}