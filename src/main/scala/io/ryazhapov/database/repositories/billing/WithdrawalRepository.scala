package io.ryazhapov.database.repositories.billing

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.billing.Withdrawal
import zio.{Has, ULayer, ZLayer}

object WithdrawalRepository extends Repository {

  import dbContext._

  type WithdrawalRepository = Has[Service]
  lazy val live: ULayer[WithdrawalRepository] =
    ZLayer.succeed(new ServiceImpl())

  trait Service {
    def create(withdrawal: Withdrawal): Result[Unit]

    def getAll: Result[List[Withdrawal]]

    def getByTeacher(teacherId: UserId): Result[List[Withdrawal]]
  }

  class ServiceImpl() extends Service {
    lazy val withdrawalTable: Quoted[EntityQuery[Withdrawal]] = quote {
      querySchema[Withdrawal](""""Withdrawal"""")
    }

    override def create(withdrawal: Withdrawal): Result[Unit] =
      dbContext.run(withdrawalTable
        .insert(lift(withdrawal))
      ).unit

    override def getAll: Result[List[Withdrawal]] =
      dbContext.run(withdrawalTable)

    override def getByTeacher(teacherId: UserId): Result[List[Withdrawal]] =
      dbContext.run(withdrawalTable
        .filter(_.teacherId == lift(teacherId))
      )
  }
}