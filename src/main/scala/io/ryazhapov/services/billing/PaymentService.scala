package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.billing.PaymentRepository
import io.ryazhapov.database.repositories.billing.PaymentRepository.PaymentRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.domain.{LessonId, PaymentId, UserId}
import io.ryazhapov.errors.PaymentNotFound
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZIO, ZLayer}

@accessible
object PaymentService {

  type PaymentService = Has[Service]

  trait Service {
    def createPayment(payment: Payment): RIO[DBTransactor, Unit]

    def getPayment(id: PaymentId): RIO[DBTransactor, Payment]

    def getAllPayments: RIO[DBTransactor, List[Payment]]

    def getPaymentByStudent(studentId: UserId): RIO[DBTransactor, List[Payment]]

    def getPaymentByTeacher(teacherId: UserId): RIO[DBTransactor, List[Payment]]

    def getPaymentByLesson(lessonId: LessonId): RIO[DBTransactor, Payment]
  }

  class ServiceImpl(
    paymentRepository: PaymentRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createPayment(payment: Payment): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- paymentRepository.create(payment).transact(transactor).unit
      } yield ()

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

  lazy val live: ZLayer[PaymentRepository, Nothing, PaymentService] =
    ZLayer.fromService[PaymentRepository.Service, PaymentService.Service](repo => new ServiceImpl(repo))
}
