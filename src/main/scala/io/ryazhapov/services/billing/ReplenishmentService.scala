package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.StudentRepository
import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.database.repositories.auth.UserRepository
import io.ryazhapov.database.repositories.auth.UserRepository.UserRepository
import io.ryazhapov.database.repositories.billing.ReplenishmentRepository
import io.ryazhapov.database.repositories.billing.ReplenishmentRepository.ReplenishmentRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Student
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.billing.Replenishment
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZLayer}

@accessible
object ReplenishmentService {

  type ReplenishmentService = Has[Service]
  lazy val live: ZLayer[ReplenishmentRepository with
    UserRepository with StudentRepository, Nothing, ReplenishmentService] =
    ZLayer.fromServices[UserRepository.Service, StudentRepository.Service, ReplenishmentRepository.Service,
      ReplenishmentService.Service]((userRepo, studentRepo, replenishmentRepo) =>
      new ServiceImpl(userRepo, studentRepo, replenishmentRepo))

  trait Service {
    def createReplenishment(user: User, student: Student, replenishment: Replenishment): RIO[DBTransactor, Unit]

    def getAllReplenishments: RIO[DBTransactor, List[Replenishment]]

    def getStudentsReplenishment(studentId: UserId): RIO[DBTransactor, List[Replenishment]]
  }

  class ServiceImpl(
    userRepository: UserRepository.Service,
    studentRepository: StudentRepository.Service,
    replenishmentRepository: ReplenishmentRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createReplenishment(
      user: User,
      student: Student,
      replenishment: Replenishment): RIO[DBTransactor, Unit] = {

      val action = for {
        _ <- userRepository.update(user)
        _ <- studentRepository.update(student)
        _ <- replenishmentRepository.create(replenishment)
      } yield ()

      for {
        transactor <- TransactorService.databaseTransactor
        _ <- action.transact(transactor).unit
      } yield ()
    }

    override def getAllReplenishments: RIO[DBTransactor, List[Replenishment]] =
      for {
        transactor <- TransactorService.databaseTransactor
        replenishment <- replenishmentRepository.getAll.transact(transactor)
      } yield replenishment

    override def getStudentsReplenishment(studentId: UserId): RIO[DBTransactor, List[Replenishment]] =
      for {
        transactor <- TransactorService.databaseTransactor
        replenishment <- replenishmentRepository.getByStudent(studentId).transact(transactor)
      } yield replenishment
  }
}
