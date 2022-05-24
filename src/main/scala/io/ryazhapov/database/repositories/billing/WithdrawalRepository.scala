package io.ryazhapov.database.repositories.billing

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.repositories.billing.PaymentRepository.dbContext
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.accounts.Teacher
import io.ryazhapov.domain.billing.Withdrawal
import io.ryazhapov.domain.{UserId, WithdrawalId}
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object WithdrawalRepository extends Repository {

  import dbContext._

  type WithdrawalRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, WithdrawalRepository] =
    ZLayer.fromService(new PostgresWithdrawalRepository(_))

  trait Service {
    def create(teacher: Teacher, withdrawal: Withdrawal): Task[WithdrawalId]

    def getAll: Task[List[Withdrawal]]

    def getByTeacher(teacherId: UserId): Task[List[Withdrawal]]
  }

  class PostgresWithdrawalRepository(xa: Transactor[Task]) extends Service {

    lazy val withdrawalTable = quote(querySchema[Withdrawal](""""Withdrawal""""))
    lazy val teacherTable = quote(querySchema[Teacher](""""Teacher""""))

    override def create(teacher: Teacher, withdrawal: Withdrawal): Task[WithdrawalId] = {

      val updateTeacher = dbContext.run {
        teacherTable
          .filter(_.userId == lift(teacher.userId))
          .update(lift(teacher))
      }.unit

      val createWithdrawal = dbContext.run {
        withdrawalTable
          .insert(lift(withdrawal))
          .returningGenerated(_.id)
      }

      val transaction = for {
        _ <- updateTeacher
        withdrawalId <- createWithdrawal
      } yield withdrawalId

      transaction.transact(xa)
    }

    override def getAll: Task[List[Withdrawal]] =
      dbContext.run(withdrawalTable).transact(xa)

    override def getByTeacher(teacherId: UserId): Task[List[Withdrawal]] =
      dbContext.run {
        withdrawalTable
          .filter(_.teacherId == lift(teacherId))
      }.transact(xa)
  }
}