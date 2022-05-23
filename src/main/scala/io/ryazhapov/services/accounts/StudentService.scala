package io.ryazhapov.services.accounts

import io.ryazhapov.database.repositories.accounts.StudentRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.{Student, StudentUpdateRequest}
import io.ryazhapov.errors.StudentNotFound
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object StudentService {

  type StudentService = Has[Service]
  lazy val live = ZLayer.fromService[
    StudentRepository.Service,
    StudentService.Service
  ](repo => new ServiceImpl(repo))

  trait Service {
    def createStudent(student: Student): Task[Unit]

    def updateStudent(id: UserId, request: StudentUpdateRequest): Task[Unit]

    def getStudent(id: UserId): Task[Student]

    def getAllStudents: Task[List[Student]]

    def deleteStudent(id: UserId): Task[Unit]
  }

  class ServiceImpl(
    studentRepository: StudentRepository.Service
  ) extends Service {

    override def createStudent(student: Student): Task[Unit] =
      studentRepository.create(student)

    override def updateStudent(id: UserId, request: StudentUpdateRequest): Task[Unit] =
      for {
        studentOpt <- studentRepository.get(id)
        student <- ZIO.fromEither(studentOpt.toRight(StudentNotFound))
        updated = student.copy(target = request.target)
        _ <- studentRepository.update(updated)
      } yield ()

    override def getStudent(id: UserId): Task[Student] =
      for {
        studentOpt <- studentRepository.get(id)
        student <- ZIO.fromEither(studentOpt.toRight(StudentNotFound))
      } yield student

    override def getAllStudents: Task[List[Student]] =
      studentRepository.getAll

    //TODO
    override def deleteStudent(id: UserId): Task[Unit] =
      studentRepository.delete(id)
  }
}