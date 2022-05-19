package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.billing.WithdrawalRepository
import io.ryazhapov.database.repositories.billing.WithdrawalRepository.WithdrawalRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.billing.Withdrawal
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZLayer}

@accessible
object WithdrawalService {

  type WithdrawalService = Has[Service]
  lazy val live: ZLayer[WithdrawalRepository, Nothing, WithdrawalService] =
    ZLayer.fromService[WithdrawalRepository.Service, WithdrawalService.Service](repo => new ServiceImpl(repo))

  trait Service {
    def createWithdrawal(withdrawal: Withdrawal): RIO[DBTransactor, Unit]

    def getAllWithdrawals: RIO[DBTransactor, List[Withdrawal]]

    def getWithdrawalByTeacher(teacherId: UserId): RIO[DBTransactor, List[Withdrawal]]
  }

  class ServiceImpl(
    withdrawalRepository: WithdrawalRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createWithdrawal(withdrawal: Withdrawal): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- withdrawalRepository.create(withdrawal).transact(transactor).unit
      } yield ()

    override def getAllWithdrawals: RIO[DBTransactor, List[Withdrawal]] =
      for {
        transactor <- TransactorService.databaseTransactor
        withdrawal <- withdrawalRepository.getAll.transact(transactor)
      } yield withdrawal

    override def getWithdrawalByTeacher(teacherId: UserId): RIO[DBTransactor, List[Withdrawal]] =
      for {
        transactor <- TransactorService.databaseTransactor
        withdrawal <- withdrawalRepository.getByTeacher(teacherId).transact(transactor)
      } yield withdrawal
  }
}
