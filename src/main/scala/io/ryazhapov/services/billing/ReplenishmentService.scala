package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.accounts.StudentRepository
import io.ryazhapov.database.repositories.auth.UserRepository
import io.ryazhapov.database.repositories.billing.ReplenishmentRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Student
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.billing.Replenishment
import zio.macros.accessible
import zio.{Has, Task, ZLayer}

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
    def createReplenishment(user: User, student: Student, replenishment: Replenishment): Task[Unit]

    def getAllReplenishments: Task[List[Replenishment]]

    def getStudentsReplenishment(studentId: UserId): Task[List[Replenishment]]
  }

  class ServiceImpl(
    userRepository: UserRepository.Service,
    studentRepository: StudentRepository.Service,
    replenishmentRepository: ReplenishmentRepository.Service
  ) extends Service {

    override def createReplenishment(user: User, student: Student, replenishment: Replenishment): Task[Unit] =
      for {
        _ <- userRepository.update(user)
        _ <- studentRepository.update(student)
        _ <- replenishmentRepository.create(replenishment)
      } yield ()

    override def getAllReplenishments: Task[List[Replenishment]] =
      replenishmentRepository.getAll

    override def getStudentsReplenishment(studentId: UserId): Task[List[Replenishment]] =
      replenishmentRepository.getByStudent(studentId)
  }
}