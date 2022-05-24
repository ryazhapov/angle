package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.TeacherRepository
import io.ryazhapov.database.repositories.billing.WithdrawalRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.billing.{Withdrawal, WithdrawalRequest}
import io.ryazhapov.errors.{NotEnoughMoney, TeacherNotFound}
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object WithdrawalService {

  type WithdrawalService = Has[Service]

  lazy val live = ZLayer.fromServices[
    TeacherRepository.Service,
    WithdrawalRepository.Service,
    WithdrawalService.Service
  ]((teacherRepo, withdrawalRepo) => new ServiceImpl(teacherRepo, withdrawalRepo))

  trait Service {
    def createWithdrawal(id: UserId, request: WithdrawalRequest): Task[Unit]

    def getAllWithdrawals: Task[List[Withdrawal]]

    def getWithdrawalByTeacher(teacherId: UserId): Task[List[Withdrawal]]
  }

  class ServiceImpl(
    teacherRepository: TeacherRepository.Service,
    withdrawalRepository: WithdrawalRepository.Service
  ) extends Service {

    override def createWithdrawal(id: UserId, request: WithdrawalRequest): Task[Unit] =
      for {
        teacherOpt <- teacherRepository.get(id)
        teacher <- ZIO.fromEither(teacherOpt.toRight(TeacherNotFound))
        _ <- ZIO.when(teacher.balance < request.amount)(ZIO.fail(NotEnoughMoney))
        updTeacher = teacher.copy(balance = teacher.balance - request.amount)
        newWithdrawal = Withdrawal(0, id, request.amount)
        _ <- withdrawalRepository.create(updTeacher, newWithdrawal)
      } yield ()

    override def getAllWithdrawals: Task[List[Withdrawal]] =
      withdrawalRepository.getAll

    override def getWithdrawalByTeacher(teacherId: UserId): Task[List[Withdrawal]] =
      withdrawalRepository.getByTeacher(teacherId)
  }
}