package io.ryazhapov.database.repositories.accounts

import io.ryazhapov.database.dao.accounts.TeacherDao
import io.ryazhapov.database.repositories.{Repository, accounts}
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Level
import zio.{Has, ULayer, ZLayer}

object TeacherRepository extends Repository {

  import dbContext._

  type TeacherRepository = Has[Service]

  private implicit val encodeLevel: MappedEncoding[Level, String] = MappedEncoding[Level, String](_.toString)
  private implicit val decodeLevel: MappedEncoding[String, Level] = MappedEncoding[String, Level](Level.fromString)

  trait Service {
    def create(teacher: TeacherDao): Result[Unit]

    def update(teacher: TeacherDao): Result[Unit]

    def get(id: UserId): Result[Option[TeacherDao]]

    def getAll: Result[List[TeacherDao]]

    def delete(id: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val teacherTable:Quoted[EntityQuery[TeacherDao]] = quote {
      querySchema[TeacherDao](""""Teacher"""")
    }

    override def create(teacher: TeacherDao): Result[Unit] =
      dbContext.run(teacherTable.insert(lift(teacher))).unit

    override def update(teacher: TeacherDao): Result[Unit] =
      dbContext.run(teacherTable.filter(_.userId == lift(teacher.userId)).update(lift(teacher))).unit

    override def get(id: UserId): Result[Option[TeacherDao]] =
      dbContext.run(teacherTable.filter(_.userId == lift(id))).map(_.headOption)

    override def getAll: Result[List[TeacherDao]] =
      dbContext.run(teacherTable)

    override def delete(id: UserId): Result[Unit] =
      dbContext.run(teacherTable.filter(_.userId == lift(id)).delete).unit
  }

  lazy val live: ULayer[TeacherRepository] =
    ZLayer.succeed(new ServiceImpl())
}
