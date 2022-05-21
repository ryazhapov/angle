package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.TeacherRepository
import io.ryazhapov.database.repositories.accounts.TeacherRepository.TeacherRepository
import io.ryazhapov.database.repositories.billing.WithdrawalRepository
import io.ryazhapov.database.repositories.billing.WithdrawalRepository.WithdrawalRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Teacher
import io.ryazhapov.domain.billing.Withdrawal
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZLayer}

@accessible
object WithdrawalService {

  type WithdrawalService = Has[Service]
  lazy val live: ZLayer[TeacherRepository with WithdrawalRepository, Nothing, WithdrawalService] =
    ZLayer.fromServices[TeacherRepository.Service, WithdrawalRepository.Service, WithdrawalService.Service](
      (teacherRepo, withdrawalRepo) => new ServiceImpl(teacherRepo, withdrawalRepo))

  trait Service {
    def createWithdrawal(teacher: Teacher, withdrawal: Withdrawal): RIO[DBTransactor, Unit]

    def getAllWithdrawals: RIO[DBTransactor, List[Withdrawal]]

    def getWithdrawalByTeacher(teacherId: UserId): RIO[DBTransactor, List[Withdrawal]]
  }

  class ServiceImpl(
    teacherRepository: TeacherRepository.Service,
    withdrawalRepository: WithdrawalRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createWithdrawal(teacher: Teacher, withdrawal: Withdrawal): RIO[DBTransactor, Unit] = {
      val action = for {
        _ <- teacherRepository.update(teacher)
        _ <- withdrawalRepository.create(withdrawal)
      } yield ()
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- action.transact(transactor)
      } yield ()
    }

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
