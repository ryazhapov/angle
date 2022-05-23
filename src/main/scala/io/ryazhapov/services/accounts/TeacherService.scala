package io.ryazhapov.services.accounts

import io.ryazhapov.database.repositories.accounts.TeacherRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.{Teacher, TeacherUpdateRequest}
import io.ryazhapov.errors.TeacherNotFound
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object TeacherService {

  type TeacherService = Has[Service]

  lazy val live = ZLayer.fromService[
    TeacherRepository.Service,
    TeacherService.Service
  ](repo => new ServiceImpl(repo))

  trait Service {
    def createTeacher(teacher: Teacher): Task[Unit]

    def updateTeacher(id: UserId, request: TeacherUpdateRequest): Task[Unit]

    def getTeacher(id: UserId): Task[Teacher]

    def getAllTeachers: Task[List[Teacher]]

    def deleteTeacher(id: UserId): Task[Unit]
  }

  class ServiceImpl(
    teacherRepository: TeacherRepository.Service
  ) extends Service {

    override def createTeacher(teacher: Teacher): Task[Unit] =
      teacherRepository.create(teacher)

    override def updateTeacher(id: UserId, request: TeacherUpdateRequest): Task[Unit] =
      for {
        teacherOpt <- teacherRepository.get(id)
        teacher <- ZIO.fromEither(teacherOpt.toRight(TeacherNotFound))
        updated = teacher.copy(rate = request.rate)
        _ <- teacherRepository.update(updated)
      } yield ()

    override def getTeacher(id: UserId): Task[Teacher] =
      for {
        teacherOpt <- teacherRepository.get(id)
        teacher <- ZIO.fromEither(teacherOpt.toRight(TeacherNotFound))
      } yield teacher

    override def getAllTeachers: Task[List[Teacher]] =
      teacherRepository.getAll

    //TODO
    override def deleteTeacher(id: UserId): Task[Unit] =
      teacherRepository.delete(id)
  }
}
