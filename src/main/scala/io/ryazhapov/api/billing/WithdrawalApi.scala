package io.ryazhapov.api.billing

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.{AdminRole, TeacherRole}
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.billing.Withdrawal
import io.ryazhapov.services.accounts.TeacherService
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import io.ryazhapov.services.billing.WithdrawalService
import io.ryazhapov.services.billing.WithdrawalService.WithdrawalService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class WithdrawalApi[R <: Api.DefaultApiEnv with WithdrawalService with TeacherService] extends Api[R] {

  import dsl._

  val withdrawalRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case authReq @ PUT -> Root / "create" as UserWithSession(user, session) =>
      user.role match {
        case TeacherRole =>
          val handleRequest = for {
            _ <- log.info(s"Creating withdrawal for ${user.id}")
            foundTeacher <- TeacherService.getTeacher(user.id)
            request <- authReq.req.as[WithdrawalRequest]
            id <- zio.random.nextUUID
            withdrawal = Withdrawal(
              id,
              user.id,
              request.amount
            )
            updatedTeacher = foundTeacher.copy(balance = foundTeacher.balance - request.amount)
            result <- TeacherService.updateTeacher(updatedTeacher) *>
              WithdrawalService.createWithdrawal(withdrawal)
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
            _ <- log.info(s"Getting all withdrawals of teacher ${user.id}")
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

  case class WithdrawalRequest(
    amount: Int
  )
}