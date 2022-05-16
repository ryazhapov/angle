package io.ryazhapov.services.accounts

import io.ryazhapov.database.repositories.accounts.TeacherRepository
import io.ryazhapov.database.repositories.accounts.TeacherRepository.TeacherRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Teacher
import io.ryazhapov.errors.TeacherNotFound
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZIO, ZLayer}

@accessible
object TeacherService {

  type TeacherService = Has[Service]

  trait Service {
    def createTeacher(teacher: Teacher): RIO[DBTransactor, Unit]

    def updateTeacher(teacher: Teacher): RIO[DBTransactor, Unit]

    def getTeacher(id: UserId): RIO[DBTransactor, Teacher]

    def getAllTeachers: RIO[DBTransactor, List[Teacher]]

    def deleteTeacher(id: UserId): RIO[DBTransactor, Unit]
  }

  class ServiceImpl(
    teacherRepository: TeacherRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createTeacher(teacher: Teacher): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- teacherRepository.create(teacher.toDao).transact(transactor).unit
      } yield ()

    override def updateTeacher(teacher: Teacher): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- teacherRepository.update(teacher.toDao).transact(transactor)
      } yield teacher

    override def getTeacher(id: UserId): RIO[DBTransactor, Teacher] =
      for {
        transactor <- TransactorService.databaseTransactor
        teacherDao <- teacherRepository.get(id).transact(transactor)
        teacher <- ZIO.fromEither(teacherDao.map(_.toTeacher).toRight(TeacherNotFound))
      } yield teacher

    override def getAllTeachers: RIO[DBTransactor, List[Teacher]] =
      for {
        transactor <- TransactorService.databaseTransactor
        teachersDao <- teacherRepository.getAll.transact(transactor)
        teachers = teachersDao.map(_.toTeacher)
      } yield teachers

    override def deleteTeacher(id: UserId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- teacherRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[TeacherRepository, Nothing, TeacherService] =
    ZLayer.fromService[TeacherRepository.Service, TeacherService.Service](repo => new ServiceImpl(repo))
}
