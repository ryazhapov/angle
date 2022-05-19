package io.ryazhapov.api.billing

import io.circe.generic.auto._
import io.ryazhapov.api.Api
import io.ryazhapov.domain.accounts.Role.TeacherRole
import io.ryazhapov.domain.auth.UserWithSession
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.errors.UnauthorizedAction
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import io.ryazhapov.services.accounts.{StudentService, TeacherService}
import io.ryazhapov.services.billing.PaymentService
import io.ryazhapov.services.billing.PaymentService.PaymentService
import io.ryazhapov.services.lessons.LessonService
import io.ryazhapov.services.lessons.LessonService.LessonService
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import zio.interop.catz._
import zio.logging._
import zio.{IO, ZIO}

class PaymentApi[R <: Api.DefaultApiEnv with PaymentService with LessonService with TeacherService] extends Api[R] {

  import dsl._

  val paymentRoutes: AuthedRoutes[UserWithSession, ApiTask] = AuthedRoutes.of[UserWithSession, ApiTask] {

    case POST -> Root / "lesson_completed" :? LessonIdParamMatcher(lessonId) as UserWithSession(user, session) =>
      user.role match {
        case TeacherRole =>
          val handleRequest = for {
            _ <- log.info(s"Creating payment for lesson {$lessonId")
            foundLesson <- LessonService.getLesson(lessonId)
            foundTeacher <- TeacherService.getTeacher(user.id)
            foundStudent <- StudentService.getStudent(foundLesson.studentId)
            _ <- ZIO.when(user.id == foundLesson.teacherId)(ZIO.fail(UnauthorizedAction))
            id <- zio.random.nextUUID
            payment = Payment(
              id,
              foundLesson.studentId,
              foundLesson.teacherId,
              foundLesson.id,
              foundTeacher.rate
            )
            updatedTeacher = foundTeacher.copy(balance = foundTeacher.balance + foundTeacher.rate)
            updatedStudent = foundStudent.copy(balance = foundStudent.balance - foundTeacher.rate)
            result <- TeacherService.updateTeacher(updatedTeacher) *>
              StudentService.updateStudent(updatedStudent) *>
              LessonService.completeLesson(lessonId) *>
              PaymentService.createPayment(payment)
          } yield result
          handleRequest.foldM(
            throwableToHttpCode,
            result => okWithCookie(result, session.id)
          )

        case _ => IO(Response(Unauthorized))
      }

    case GET -> Root / UUIDVar(id) as UserWithSession(_, session) =>
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