package io.ryazhapov.database.repositories.accounts

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Teacher
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object TeacherRepository extends Repository {

  import dbContext._

  type TeacherRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, TeacherRepository] =
    ZLayer.fromService(new PostgresTeacherRepository(_))

  trait Service {
    def create(teacher: Teacher): Task[Unit]

    def update(teacher: Teacher): Task[Unit]

    def get(id: UserId): Task[Option[Teacher]]

    def getAll: Task[List[Teacher]]

    def delete(id: UserId): Task[Unit]
  }

  class PostgresTeacherRepository(xa: Transactor[Task]) extends Service {

    lazy val teacherTable = quote(querySchema[Teacher](""""Teacher""""))

    override def create(teacher: Teacher): Task[Unit] =
      dbContext.run {
        teacherTable
          .insert(lift(teacher))
      }.unit.transact(xa)

    override def update(teacher: Teacher): Task[Unit] =
      dbContext.run {
        teacherTable
          .filter(_.userId == lift(teacher.userId))
          .update(lift(teacher))
      }.unit.transact(xa)

    override def get(id: UserId): Task[Option[Teacher]] =
      dbContext.run {
        teacherTable
          .filter(_.userId == lift(id))
      }.map(_.headOption).transact(xa)

    override def getAll: Task[List[Teacher]] =
      dbContext.run(teacherTable).transact(xa)

    override def delete(id: UserId): Task[Unit] =
      dbContext.run {
        teacherTable
          .filter(_.userId == lift(id))
          .delete
      }.unit.transact(xa)
  }
}