package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.database.repositories.accounts.TeacherRepository.TeacherRepository
import io.ryazhapov.database.repositories.accounts.{StudentRepository, TeacherRepository}
import io.ryazhapov.database.repositories.billing.PaymentRepository
import io.ryazhapov.database.repositories.billing.PaymentRepository.PaymentRepository
import io.ryazhapov.database.repositories.lessons.LessonRepository
import io.ryazhapov.database.repositories.lessons.LessonRepository.LessonRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.accounts.{Student, Teacher}
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.domain.lessons.Lesson
import io.ryazhapov.domain.{LessonId, PaymentId, UserId}
import io.ryazhapov.errors.PaymentNotFound
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, RLayer, ZIO, ZLayer}

@accessible
object PaymentService {

  type PaymentService = Has[Service]

  lazy val live: RLayer[StudentRepository with TeacherRepository with
    LessonRepository with PaymentRepository, PaymentService] = ZLayer.fromServices[
    StudentRepository.Service, TeacherRepository.Service,
    LessonRepository.Service, PaymentRepository.Service, PaymentService.Service] {
    (studentRepo, teacherRepo, lessonRepo, paymentRepo) =>
      new ServiceImpl(studentRepo, teacherRepo, lessonRepo, paymentRepo)
  }

  trait Service {
    def createPayment(student: Student, teacher: Teacher, lesson: Lesson, payment: Payment): RIO[DBTransactor, Unit]

    def getPayment(id: PaymentId): RIO[DBTransactor, Payment]

    def getAllPayments: RIO[DBTransactor, List[Payment]]

    def getPaymentByStudent(studentId: UserId): RIO[DBTransactor, List[Payment]]

    def getPaymentByTeacher(teacherId: UserId): RIO[DBTransactor, List[Payment]]

    def getPaymentByLesson(lessonId: LessonId): RIO[DBTransactor, Payment]
  }

  class ServiceImpl(
    studentRepository: StudentRepository.Service,
    teacherRepository: TeacherRepository.Service,
    lessonRepository: LessonRepository.Service,
    paymentRepository: PaymentRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createPayment(
      student: Student,
      teacher: Teacher, lesson: Lesson,
      payment: Payment): RIO[DBTransactor, Unit] = {

      val action = for {
        _ <- studentRepository.update(student)
        _ <- teacherRepository.update(teacher)
        _ <- lessonRepository.update(lesson)
        _ <- paymentRepository.create(payment)
      } yield ()

      for {
        transactor <- TransactorService.databaseTransactor
        _ <- action.transact(transactor)
      } yield ()
    }

    override def getPayment(id: PaymentId): RIO[DBTransactor, Payment] =
      for {
        transactor <- TransactorService.databaseTransactor
        paymentOpt <- paymentRepository.get(id).transact(transactor)
        payment <- ZIO.fromEither(paymentOpt.toRight(PaymentNotFound))
      } yield payment

    override def getAllPayments: RIO[DBTransactor, List[Payment]] =
      for {
        transactor <- TransactorService.databaseTransactor
        payment <- paymentRepository.getAll.transact(transactor)
      } yield payment

    override def getPaymentByStudent(studentId: UserId): RIO[DBTransactor, List[Payment]] =
      for {
        transactor <- TransactorService.databaseTransactor
        payment <- paymentRepository.getByStudent(studentId).transact(transactor)
      } yield payment

    override def getPaymentByTeacher(teacherId: UserId): RIO[DBTransactor, List[Payment]] =
      for {
        transactor <- TransactorService.databaseTransactor
        payment <- paymentRepository.getByTeacher(teacherId).transact(transactor)
      } yield payment

    override def getPaymentByLesson(lessonId: LessonId): RIO[DBTransactor, Payment] =
      for {
        transactor <- TransactorService.databaseTransactor
        paymentOpt <- paymentRepository.getByLesson(lessonId).transact(transactor)
        payment <- ZIO.fromEither(paymentOpt.toRight(PaymentNotFound))
      } yield payment
  }
}
