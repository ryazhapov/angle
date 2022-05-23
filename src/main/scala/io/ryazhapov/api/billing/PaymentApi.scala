package io.ryazhapov.api.billing

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.TeacherRole
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import io.ryazhapov.services.billing.PaymentService
import io.ryazhapov.services.billing.PaymentService.PaymentService
import io.ryazhapov.services.lessons.LessonService.LessonService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.IO
import zio.interop.catz._
import zio.logging._

class PaymentApi[R <: Api.DefaultApiEnv with PaymentService with LessonService with TeacherService] extends Api[R] {

  import dsl._

  val paymentRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case POST -> Root / "lesson_completed" :? LessonIdParamMatcher(lessonId) as UserWithSession(user, session) =>
      user.role match {
        case TeacherRole if user.verified =>
          val handleRequest = for {
            _ <- log.info(s"Creating payment for lesson {$lessonId")
            result <- PaymentService.createPayment(user.id, lessonId)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / IntVar(id) as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting payment $id")
        result <- PaymentService.getPayment(id)
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )

    case GET -> Root as UserWithSession(_, session) =>
      val handleRequest = for {
        _ <- log.info(s"Getting all payments")
        result <- PaymentService.getAllPayments
      } yield result
      handleRequest.foldM(
        throwableToHttpCode,
        result => okWithCookie(result, session.id)
      )
  }

  override def routes: HttpRoutes[ApiTask] =
    authMiddleware(paymentRoutes)
}