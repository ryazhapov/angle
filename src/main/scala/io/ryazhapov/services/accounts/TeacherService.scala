package io.ryazhapov.services.accounts

import io.ryazhapov.database.repositories.accounts.TeacherRepository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Teacher
import io.ryazhapov.errors.TeacherNotFound
import zio.macros.accessible
import zio.{Has, RIO, Task, ZIO, ZLayer}

@accessible
object TeacherService {

  type TeacherService = Has[Service]

  lazy val live = ZLayer.fromService[
    TeacherRepository.Service,
    TeacherService.Service
  ](repo => new ServiceImpl(repo))

  trait Service {
    def createTeacher(teacher: Teacher): Task[Unit]

    def updateTeacher(teacher: Teacher): Task[Unit]

    def getTeacher(id: UserId): Task[Teacher]

    def getAllTeachers: Task[List[Teacher]]

    def deleteTeacher(id: UserId): Task[Unit]
  }

  class ServiceImpl(
    teacherRepository: TeacherRepository.Service
  ) extends Service {

    override def createTeacher(teacher: Teacher): Task[Unit] =
      teacherRepository.create(teacher)

    override def updateTeacher(teacher: Teacher): Task[Unit] =
      teacherRepository.update(teacher)

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
