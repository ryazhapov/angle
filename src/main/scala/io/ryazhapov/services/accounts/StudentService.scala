package io.ryazhapov.services.accounts

import io.ryazhapov.database.repositories.accounts.StudentRepository
import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Student
import io.ryazhapov.errors.{StudentNotFound, TeacherNotFound}
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZIO, ZLayer}

@accessible
object StudentService {

  type StudentService = Has[Service]

  trait Service {
    def createStudent(student: Student): RIO[DBTransactor, Unit]

    def updateStudent(student: Student): RIO[DBTransactor, Unit]

    def getStudent(id: UserId): RIO[DBTransactor, Student]

    def getAllStudents: RIO[DBTransactor, List[Student]]

    def deleteStudent(id: UserId): RIO[DBTransactor, Unit]
  }

  class ServiceImpl(
    studentRepository: StudentRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createStudent(student: Student): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- studentRepository.create(student.toDao).transact(transactor).unit
      } yield ()

    override def updateStudent(student: Student): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- studentRepository.update(student.toDao).transact(transactor)
      } yield student

    override def getStudent(id: UserId): RIO[DBTransactor, Student] =
      for {
        transactor <- TransactorService.databaseTransactor
        studentDao <- studentRepository.get(id).transact(transactor)
        student <- ZIO.fromEither(studentDao.map(_.toStudent).toRight(StudentNotFound))
      } yield student

    override def getAllStudents: RIO[DBTransactor, List[Student]] =
      for {
        transactor <- TransactorService.databaseTransactor
        studentDao <- studentRepository.getAll.transact(transactor)
        students = studentDao.map(_.toStudent)
      } yield students

    override def deleteStudent(id: UserId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- studentRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[StudentRepository, Nothing, StudentService] =
    ZLayer.fromService[StudentRepository.Service, StudentService.Service](repo => new ServiceImpl(repo))
}
