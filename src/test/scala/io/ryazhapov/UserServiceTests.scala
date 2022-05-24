package io.ryazhapov

import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.database.repositories.auth.UserRepository.UserRepository
import io.ryazhapov.database.repositories.billing.WithdrawalRepository.WithdrawalRepository
import io.ryazhapov.database.repositories.lessons.LessonRepository.LessonRepository
import io.ryazhapov.database.repositories.lessons.ScheduleRepository.ScheduleRepository
import io.ryazhapov.domain.accounts.Role.{AdminRole, StudentRole, TeacherRole}
import io.ryazhapov.domain.accounts.Student
import io.ryazhapov.domain.auth.{SignUpRequest, User}
import io.ryazhapov.domain.billing.{ReplenishmentRequest, Withdrawal, WithdrawalRequest}
import io.ryazhapov.domain.lessons.{Lesson, LessonRequest, Schedule, ScheduleRequest}
import io.ryazhapov.repositories._
import io.ryazhapov.services.accounts.{AdminService, StudentService, TeacherService}
import io.ryazhapov.services.auth.UserService
import io.ryazhapov.services.billing.{PaymentService, ReplenishmentService, WithdrawalService}
import io.ryazhapov.services.lessons.{LessonService, ScheduleService}
import zio.ZIO
import zio.test.Assertion.{equalTo, exists, hasField, isSome}
import zio.test._

import java.time.ZonedDateTime

object UserServiceTests extends DefaultRunnableSpec {
  override def spec = suite("user service tests")(

    testM("student can sign up") {
      val request = SignUpRequest("student@gmail.com", "password", StudentRole)
      for {
        response <- UserService.createUser(request)
        user <- UserService.getUser(response)
        saved <- ZIO.accessM[UserRepository](_.get.get(user.id))
      } yield assert(saved)(
        isSome(
          hasField("email", (e: User) => e.email, equalTo(request.email)) &&
            hasField(
              "role",
              (e: User) => e.role,
              equalTo(request.role)
            )
        )
      )
    }.provideSomeLayer(layer()),

    testM("teacher can sign up") {
      val request = SignUpRequest("teacher@gmail.com", "password", TeacherRole)
      for {
        response <- UserService.createUser(request)
        user <- UserService.getUser(response)
        saved <- ZIO.accessM[UserRepository](_.get.get(user.id))
      } yield assert(saved)(
        isSome(
          hasField("email", (e: User) => e.email, equalTo(request.email)) &&
            hasField(
              "role",
              (e: User) => e.role,
              equalTo(request.role)
            )
        )
      )
    }.provideSomeLayer(layer()),

    testM("admin can sign up") {

      val request = SignUpRequest("admin@gmail.com", "password", AdminRole)
      for {
        response <- UserService.createUser(request)
        user <- UserService.getUser(response)
        saved <- ZIO.accessM[UserRepository](_.get.get(user.id))
      } yield assert(saved)(
        isSome(
          hasField("email", (e: User) => e.email, equalTo(request.email)) &&
            hasField(
              "role",
              (e: User) => e.role,
              equalTo(request.role)
            )
        )
      )
    }.provideSomeLayer(layer()),

    testM("schedule can be created") {

      val request = ScheduleRequest(
        ZonedDateTime.parse("2022-01-01T10:00:00Z[UTC]"),
        ZonedDateTime.parse("2022-01-01T20:00:00Z[UTC]"))
      for {
        response <- ScheduleService.createSchedule(0, request)
        schedule <- ScheduleService.getSchedule(response)
        saved <- ZIO.accessM[ScheduleRepository](_.get.get(schedule.id))
      } yield assert(saved)(
        isSome(
          hasField("startsAt", (e: Schedule) => e.startsAt, equalTo(request.startsAt)) &&
            hasField(
              "endsAt", (e: Schedule) => e.endsAt, equalTo(request.endsAt)
            )
        )
      )
    }.provideSomeLayer(layer()),

    testM("student can replenish") {
      val replSum = 5000
      val studentReq = SignUpRequest("student@gmail.com", "password", StudentRole)
      for {
        _ <- UserService.createUser(studentReq)
        _ <- ReplenishmentService.createReplenishment(0, ReplenishmentRequest(replSum))
        saved <- ZIO.accessM[StudentRepository](_.get.get(0))
      } yield assert(saved)(
        isSome(
          hasField("balance", (e: Student) => e.balance, equalTo(replSum))
        )
      )
    }.provideSomeLayer(layer()),

    testM("lesson can be created") {
      val studentReq = SignUpRequest("student@gmail.com", "password", StudentRole)
      val teacherReq = SignUpRequest("teacher@gmail.com", "password", TeacherRole)
      val lessonReq = LessonRequest(0, ZonedDateTime.parse("2022-01-01T11:00:00Z[UTC]"))
      val scheduleReq = ScheduleRequest(
        ZonedDateTime.parse("2022-01-01T10:00:00Z[UTC]"),
        ZonedDateTime.parse("2022-01-01T20:00:00Z[UTC]"))
      for {
        _ <- UserService.createUser(studentReq)
        _ <- UserService.createUser(teacherReq)
        _ <- ReplenishmentService.createReplenishment(0, ReplenishmentRequest(5000))
        _ <- ScheduleService.createSchedule(0, scheduleReq)
        response <- LessonService.createLesson(0, lessonReq)
        lesson <- LessonService.getLesson(response)
        saved <- ZIO.accessM[LessonRepository](_.get.get(lesson.id))
      } yield assert(saved)(
        isSome(
          hasField("startsAt", (e: Lesson) => e.startsAt, equalTo(lessonReq.startsAt)) &&
            hasField("teacherId", (e: Lesson) => e.teacherId, equalTo(lessonReq.teacherId))
        )
      )
    }.provideSomeLayer(layer()),

    testM("teacher can withdraw") {

      val studentReq = SignUpRequest("student@gmail.com", "password", StudentRole)
      val teacherReq = SignUpRequest("teacher@gmail.com", "password", TeacherRole)
      val lessonReq = LessonRequest(0, ZonedDateTime.parse("2022-01-01T11:00:00Z[UTC]"))
      val scheduleReq = ScheduleRequest(
        ZonedDateTime.parse("2022-01-01T10:00:00Z[UTC]"),
        ZonedDateTime.parse("2022-01-01T20:00:00Z[UTC]"))
      for {
        _ <- UserService.createUser(studentReq)
        _ <- UserService.createUser(teacherReq)
        _ <- ReplenishmentService.createReplenishment(0, ReplenishmentRequest(5000))
        _ <- ScheduleService.createSchedule(0, scheduleReq)
        _ <- LessonService.createLesson(0, lessonReq)
        _ <- PaymentService.createPayment(0, 0)
        _ <- WithdrawalService.createWithdrawal(0, WithdrawalRequest(500))
        teacher <- TeacherService.getTeacher(0)
        saved <- ZIO.accessM[WithdrawalRepository](_.get.getByTeacher(teacher.userId))
      } yield assert(saved)(
        exists(
          hasField("amount", (e: Withdrawal) => e.amount, equalTo(teacher.rate))
        )
      )
    }.provideSomeLayer(layer())
  )

  private def layer() = {
    val userRepo = InMemoryUserRepository.live()
    val sessionRepo = InMemorySessionRepository.live()
    val studentRepo = InMemoryStudentRepository.live()
    val teacherRepo = InMemoryTeacherRepository.live()
    val adminRepo = InMemoryAdminRepository.live()
    val scheduleRepo = InMemoryScheduleRepository.live()
    val lessonRepo = InMemoryLessonRepository.live()
    val paymentRepo = InMemoryPaymentRepository.live()
    val withdrawalRepo = InMemoryWithdrawalRepository.live()
    val replenishmentRepo = InMemoryReplenishmentRepository.live()

    userRepo >+> sessionRepo >+> studentRepo >+> teacherRepo >+> adminRepo >+>
      scheduleRepo >+> lessonRepo >+> paymentRepo >+> withdrawalRepo >+>
      replenishmentRepo >+> UserService.live >+> StudentService.live >+>
      TeacherService.live >+> AdminService.live >+> ScheduleService.live >+>
      LessonService.live >+> PaymentService.live >+> WithdrawalService.live >+>
      ReplenishmentService.live
  }
}