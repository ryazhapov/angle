package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.{StudentRepository, TeacherRepository}
import io.ryazhapov.database.repositories.billing.PaymentRepository
import io.ryazhapov.database.repositories.lessons.LessonRepository
import io.ryazhapov.domain.accounts.{Student, Teacher}
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.domain.lessons.Lesson
import io.ryazhapov.domain.{LessonId, PaymentId, UserId}
import io.ryazhapov.errors.PaymentNotFound
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
    def createPayment(student: Student, teacher: Teacher, lesson: Lesson, payment: Payment): Task[Unit]

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

    override def createPayment(student: Student, teacher: Teacher, lesson: Lesson, payment: Payment): Task[Unit] = {
      for {
        _ <- studentRepository.update(student)
        _ <- teacherRepository.update(teacher)
        _ <- lessonRepository.update(lesson)
        _ <- paymentRepository.create(payment)
      } yield ()
    }

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
