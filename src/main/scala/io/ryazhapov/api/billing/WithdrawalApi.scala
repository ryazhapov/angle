package io.ryazhapov.api.billing

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.{AdminRole, TeacherRole}
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.billing.WithdrawalRequest
import io.ryazhapov.services.billing.WithdrawalService
import io.ryazhapov.services.billing.WithdrawalService.WithdrawalService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class WithdrawalApi[R <: Api.DefaultApiEnv with WithdrawalService] extends Api[R] {

  import dsl._

  val withdrawalRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ PUT -> Root / "create" as UserWithSession(user, session) =>
      user.role match {
        case TeacherRole if user.verified =>
          val handleRequest = for {
            _ <- log.info(s"Creating withdrawal for ${user.email}")
            withdrawReq <- authReq.req.as[WithdrawalRequest]
            result <- WithdrawalService.createWithdrawal(user.id, withdrawReq)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root as UserWithSession(user, session) =>
      user.role match {
        case AdminRole   =>
          val handleRequest = for {
            _ <- log.info(s"Getting all withdrawals")
            result <- WithdrawalService.getAllWithdrawals
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )
        case TeacherRole =>
          val handleRequest = for {
            _ <- log.info(s"Getting all withdrawals of teacher ${user.email}")
            result <- WithdrawalService.getWithdrawalByTeacher(user.id)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(withdrawalRoutes)
}