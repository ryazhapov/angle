package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.StudentRepository
import io.ryazhapov.database.repositories.auth.UserRepository
import io.ryazhapov.database.repositories.billing.ReplenishmentRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.billing.{Replenishment, ReplenishmentRequest}
import io.ryazhapov.errors.{StudentNotFound, UserNotFound}
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object ReplenishmentService {

  type ReplenishmentService = Has[Service]

  lazy val live = ZLayer.fromServices[
    UserRepository.Service,
    StudentRepository.Service,
    ReplenishmentRepository.Service,
    ReplenishmentService.Service
  ]((userRepo, studentRepo, replenishmentRepo) =>
    new ServiceImpl(userRepo, studentRepo, replenishmentRepo))

  trait Service {
    def createReplenishment(id: UserId, request: ReplenishmentRequest): Task[Unit]

    def getAllReplenishments: Task[List[Replenishment]]

    def getStudentsReplenishment(studentId: UserId): Task[List[Replenishment]]
  }

  class ServiceImpl(
    userRepository: UserRepository.Service,
    studentRepository: StudentRepository.Service,
    replenishmentRepository: ReplenishmentRepository.Service
  ) extends Service {

    override def createReplenishment(id: UserId, request: ReplenishmentRequest): Task[Unit] =
      for {
        studentOpt <- studentRepository.get(id)
        student <- ZIO.fromEither(studentOpt.toRight(StudentNotFound))
        userOpt <- userRepository.get(id)
        user <- ZIO.fromEither(userOpt.toRight(UserNotFound))
        updStudent = student.copy(balance = student.balance + request.amount)
        updUser = user.copy(verified = true)
        newReplenishment = Replenishment(0, id, request.amount)
        _ <- replenishmentRepository.create(updUser, updStudent, newReplenishment)
      } yield ()

    override def getAllReplenishments: Task[List[Replenishment]] =
      replenishmentRepository.getAll

    override def getStudentsReplenishment(studentId: UserId): Task[List[Replenishment]] =
      replenishmentRepository.getByStudent(studentId)
  }
}