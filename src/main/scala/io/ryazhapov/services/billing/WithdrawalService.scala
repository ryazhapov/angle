package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.TeacherRepository
import io.ryazhapov.database.repositories.billing.WithdrawalRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Teacher
import io.ryazhapov.domain.billing.Withdrawal
import zio.macros.accessible
import zio.{Has, Task, ZLayer}

@accessible
object WithdrawalService {

  type WithdrawalService = Has[Service]

  lazy val live = ZLayer.fromServices[
    TeacherRepository.Service,
    WithdrawalRepository.Service,
    WithdrawalService.Service
  ]((teacherRepo, withdrawalRepo) => new ServiceImpl(teacherRepo, withdrawalRepo))

  trait Service {
    def createWithdrawal(teacher: Teacher, withdrawal: Withdrawal): Task[Unit]

    def getAllWithdrawals: Task[List[Withdrawal]]

    def getWithdrawalByTeacher(teacherId: UserId): Task[List[Withdrawal]]
  }

  class ServiceImpl(
    teacherRepository: TeacherRepository.Service,
    withdrawalRepository: WithdrawalRepository.Service
  ) extends Service {

    override def createWithdrawal(teacher: Teacher, withdrawal: Withdrawal): Task[Unit] =
      for {
        _ <- teacherRepository.update(teacher)
        _ <- withdrawalRepository.create(withdrawal)
      } yield ()

    override def getAllWithdrawals: Task[List[Withdrawal]] =
      withdrawalRepository.getAll

    override def getWithdrawalByTeacher(teacherId: UserId): Task[List[Withdrawal]] =
      withdrawalRepository.getByTeacher(teacherId)
  }
}