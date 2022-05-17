package io.ryazhapov.database.repositories.accounts

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.{Level, Teacher}
import zio.{Has, ULayer, ZLayer}

object TeacherRepository extends Repository {

  import dbContext._

  type TeacherRepository = Has[Service]

  private implicit val encodeLevel: MappedEncoding[Level, String] = MappedEncoding[Level, String](_.toString)
  private implicit val decodeLevel: MappedEncoding[String, Level] = MappedEncoding[String, Level](Level.fromString)

  trait Service {
    def create(teacher: Teacher): Result[Unit]

    def update(teacher: Teacher): Result[Unit]

    def get(id: UserId): Result[Option[Teacher]]

    def getAll: Result[List[Teacher]]

    def delete(id: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val teacherTable: Quoted[EntityQuery[Teacher]] = quote {
      querySchema[Teacher](""""Teacher"""")
    }

    override def create(teacher: Teacher): Result[Unit] =
      dbContext.run(teacherTable.insert(lift(teacher))).unit

    override def update(teacher: Teacher): Result[Unit] =
      dbContext.run(teacherTable.filter(_.userId == lift(teacher.userId)).update(lift(teacher))).unit

    override def get(id: UserId): Result[Option[Teacher]] =
      dbContext.run(teacherTable.filter(_.userId == lift(id))).map(_.headOption)

    override def getAll: Result[List[Teacher]] =
      dbContext.run(teacherTable)

    override def delete(id: UserId): Result[Unit] =
      dbContext.run(teacherTable.filter(_.userId == lift(id)).delete).unit
  }

  lazy val live: ULayer[TeacherRepository] =
    ZLayer.succeed(new ServiceImpl())
}
