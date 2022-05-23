package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.{StudentRepository, TeacherRepository}
import io.ryazhapov.database.repositories.billing.PaymentRepository
import io.ryazhapov.database.repositories.lessons.LessonRepository
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.domain.{LessonId, PaymentId, UserId}
import io.ryazhapov.errors._
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object PaymentService {

  type PaymentService = Has[Service]

  lazy val live = ZLayer.fromServices[
    StudentRepository.Service,
    TeacherRepository.Service,
    LessonRepository.Service,
    PaymentRepository.Service,
    PaymentService.Service
  ]((studentRepo, teacherRepo, lessonRepo, paymentRepo) =>
    new ServiceImpl(studentRepo, teacherRepo, lessonRepo, paymentRepo))

  trait Service {
    def createPayment(id: UserId, lessonId: LessonId): Task[PaymentId]

    def getPayment(id: PaymentId): Task[Payment]

    def getAllPayments: Task[List[Payment]]

    def getPaymentByStudent(studentId: UserId): Task[List[Payment]]

    def getPaymentByTeacher(teacherId: UserId): Task[List[Payment]]

    def getPaymentByLesson(lessonId: LessonId): Task[Payment]
  }

  class ServiceImpl(
    studentRepository: StudentRepository.Service,
    teacherRepository: TeacherRepository.Service,
    lessonRepository: LessonRepository.Service,
    paymentRepository: PaymentRepository.Service
  ) extends Service {

    override def createPayment(id: UserId, lessonId: LessonId): Task[PaymentId] =
      for {
        lessonOpt <- lessonRepository.get(lessonId)
        lesson <- ZIO.fromEither(lessonOpt.toRight(LessonNotFound))
        _ <- ZIO.when(id == lesson.teacherId)(ZIO.fail(UnauthorizedAction))
        studentOpt <- studentRepository.get(lesson.studentId)
        student <- ZIO.fromEither(studentOpt.toRight(StudentNotFound))
        teacherOpt <- teacherRepository.get(id)
        teacher <- ZIO.fromEither(teacherOpt.toRight(TeacherNotFound))
        updStudent = student.copy(balance = student.reserved - teacher.rate)
        updTeacher = teacher.copy(balance = teacher.balance + teacher.rate)
        updLesson = lesson.copy(completed = true)
        _ <- studentRepository.update(updStudent)
        _ <- teacherRepository.update(updTeacher)
        _ <- lessonRepository.update(updLesson)
        paymentId <- paymentRepository.create(Payment(0, student.userId, id, lessonId, teacher.rate))
      } yield paymentId

    override def getPayment(id: PaymentId): Task[Payment] =
      for {
        paymentOpt <- paymentRepository.get(id)
        payment <- ZIO.fromEither(paymentOpt.toRight(PaymentNotFound))
      } yield payment

    override def getAllPayments: Task[List[Payment]] =
      paymentRepository.getAll

    override def getPaymentByStudent(studentId: UserId): Task[List[Payment]] =
      paymentRepository.getByStudent(studentId)

    override def getPaymentByTeacher(teacherId: UserId): Task[List[Payment]] =
      paymentRepository.getByTeacher(teacherId)

    override def getPaymentByLesson(lessonId: LessonId): Task[Payment] =
      for {
        paymentOpt <- paymentRepository.getByLesson(lessonId)
        payment <- ZIO.fromEither(paymentOpt.toRight(PaymentNotFound))
      } yield payment
  }
}
