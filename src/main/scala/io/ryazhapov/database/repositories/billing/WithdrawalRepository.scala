package io.ryazhapov.database.repositories.billing

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.repositories.lessons.ScheduleRepository.ScheduleRepository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.billing.Withdrawal
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object WithdrawalRepository extends Repository {

  import dbContext._

  type WithdrawalRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, WithdrawalRepository] =
    ZLayer.fromService(new PostgresWithdrawalRepository(_))

  trait Service {
    def create(withdrawal: Withdrawal): Task[Unit]

    def getAll: Task[List[Withdrawal]]

    def getByTeacher(teacherId: UserId): Task[List[Withdrawal]]
  }

  class PostgresWithdrawalRepository(xa: Transactor[Task]) extends Service {

    lazy val withdrawalTable = quote(querySchema[Withdrawal](""""Withdrawal""""))

    override def create(withdrawal: Withdrawal): Task[Unit] =
      dbContext.run {
        withdrawalTable
          .insert(lift(withdrawal))
      }.unit.transact(xa)

    override def getAll: Task[List[Withdrawal]] =
      dbContext.run(withdrawalTable).transact(xa)

    override def getByTeacher(teacherId: UserId): Task[List[Withdrawal]] =
      dbContext.run {
        withdrawalTable
          .filter(_.teacherId == lift(teacherId))
      }.transact(xa)
  }
}