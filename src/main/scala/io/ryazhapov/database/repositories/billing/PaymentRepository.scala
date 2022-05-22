package io.ryazhapov.database.repositories.billing

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.repositories.lessons.ScheduleRepository.ScheduleRepository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.domain.{LessonId, PaymentId, UserId}
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object PaymentRepository extends Repository {

  import dbContext._

  type PaymentRepository = Has[Service]

  lazy val live : URLayer[DBTransactor, PaymentRepository]
  = ZLayer.fromService(new PostgresPaymentRepository(_))

  trait Service {
    def create(payment: Payment): Task[Unit]

    def get(id: PaymentId): Task[Option[Payment]]

    def getAll: Task[List[Payment]]

    def getByStudent(studentId: UserId): Task[List[Payment]]

    def getByTeacher(teacherId: UserId): Task[List[Payment]]

    def getByLesson(lessonId: LessonId): Task[Option[Payment]]
  }

  class PostgresPaymentRepository(xa: Transactor[Task]) extends Service {

    lazy val paymentTable = quote(querySchema[Payment](""""Payment""""))

    override def create(payment: Payment): Task[Unit] =
      dbContext.run {
        paymentTable
          .insert(lift(payment))
      }.unit.transact(xa)

    override def get(id: PaymentId): Task[Option[Payment]] =
      dbContext.run {
        paymentTable
          .filter(_.id == lift(id))
      }.map(_.headOption).transact(xa)

    override def getAll: Task[List[Payment]] =
      dbContext.run(paymentTable).transact(xa)

    override def getByStudent(studentId: UserId): Task[List[Payment]] =
      dbContext.run {
        paymentTable
          .filter(_.studentId == lift(studentId))
      }.transact(xa)

    override def getByTeacher(teacherId: UserId): Task[List[Payment]] =
      dbContext.run {
        paymentTable
          .filter(_.teacherId == lift(teacherId))
      }.transact(xa)

    override def getByLesson(lessonId: LessonId): Task[Option[Payment]] =
      dbContext.run {
        paymentTable
          .filter(_.lessonId == lift(lessonId))
      }.map(_.headOption).transact(xa)
  }
}