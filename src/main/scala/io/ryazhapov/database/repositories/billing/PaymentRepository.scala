package io.ryazhapov.database.repositories.billing

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.domain.{LessonId, PaymentId, UserId}
import zio.{Has, ULayer, ZLayer}

object PaymentRepository extends Repository {

  import dbContext._

  type PaymentRepository = Has[Service]
  lazy val live: ULayer[PaymentRepository] =
    ZLayer.succeed(new ServiceImpl())

  trait Service {
    def create(payment: Payment): Result[Unit]

    def get(id: PaymentId): Result[Option[Payment]]

    def getAll: Result[List[Payment]]

    def getByStudent(studentId: UserId): Result[List[Payment]]

    def getByTeacher(teacherId: UserId): Result[List[Payment]]

    def getByLesson(lessonId: LessonId): Result[Option[Payment]]
  }

  class ServiceImpl() extends Service {
    lazy val paymentTable = quote {
      querySchema[Payment](""""Payment"""")
    }

    override def create(payment: Payment): Result[Unit] =
      dbContext.run(paymentTable
        .insert(lift(payment))
      ).unit

    override def get(id: PaymentId): Result[Option[Payment]] =
      dbContext.run(paymentTable
        .filter(_.id == lift(id))
      ).map(_.headOption)

    override def getAll: Result[List[Payment]] =
      dbContext.run(paymentTable)

    override def getByStudent(studentId: UserId): Result[List[Payment]] =
      dbContext.run(paymentTable
        .filter(_.studentId == lift(studentId))
      )

    override def getByTeacher(teacherId: UserId): Result[List[Payment]] =
      dbContext.run(paymentTable
        .filter(_.teacherId == lift(teacherId))
      )

    override def getByLesson(lessonId: LessonId): Result[Option[Payment]] =
      dbContext.run(paymentTable
        .filter(_.lessonId == lift(lessonId))
      ).map(_.headOption)
  }
}