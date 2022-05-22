package io.ryazhapov.database.repositories.accounts

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Student
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object StudentRepository extends Repository {

  import dbContext._

  type StudentRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, StudentRepository] =
    ZLayer.fromService(new PostgresStudentRepository(_))

  trait Service {
    def create(student: Student): Task[Unit]

    def update(student: Student): Task[Unit]

    def get(id: UserId): Task[Option[Student]]

    def getAll: Task[List[Student]]

    def delete(id: UserId): Task[Unit]
  }

  class PostgresStudentRepository(xa: Transactor[Task]) extends Service {

    lazy val StudentTable = quote(querySchema[Student](""""Student""""))

    override def create(student: Student): Task[Unit] =
      dbContext.run {
        StudentTable
          .insert(lift(student))
      }.unit.transact(xa)

    override def update(student: Student): Task[Unit] =
      dbContext.run {
        StudentTable
          .update(lift(student))
      }.unit.transact(xa)

    override def get(id: UserId): Task[Option[Student]] =
      dbContext.run {
        StudentTable
          .filter(_.userId == lift(id))
      }.map(_.headOption).transact(xa)

    override def getAll: Task[List[Student]] =
      dbContext.run(StudentTable).transact(xa)

    override def delete(id: UserId): Task[Unit] =
      dbContext.run {
        StudentTable
          .filter(_.userId == lift(id))
          .delete
      }.unit.transact(xa)
  }
}
