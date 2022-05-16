package io.ryazhapov.database.repositories.accounts

import io.ryazhapov.database.dao.accounts.StudentDao
import io.ryazhapov.database.repositories.{Repository, accounts}
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Level
import zio.{Has, ULayer, ZLayer}

object StudentRepository extends Repository {

  import dbContext._

  type StudentRepository = Has[Service]

  private implicit val encodeLevel: MappedEncoding[Level, String] = MappedEncoding[Level, String](_.toString)
  private implicit val decodeLevel: MappedEncoding[String, Level] = MappedEncoding[String, Level](Level.fromString)

  trait Service {
    def create(student: StudentDao): Result[Unit]

    def update(student: StudentDao): Result[Unit]

    def get(id: UserId): Result[Option[StudentDao]]

    def getAll: Result[List[StudentDao]]

    def delete(id: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val StudentTable: Quoted[EntityQuery[StudentDao]] = quote {
      querySchema[StudentDao](""""Student"""")
    }

    override def create(student: StudentDao): Result[Unit] =
      dbContext.run(StudentTable.insert(lift(student))).unit

    override def update(student: StudentDao): Result[Unit] =
      dbContext.run(StudentTable.update(lift(student))).unit

    override def get(id: UserId): Result[Option[StudentDao]] =
      dbContext.run(StudentTable.filter(_.userId == lift(id))).map(_.headOption)

    override def getAll: Result[List[StudentDao]] =
      dbContext.run(StudentTable)

    override def delete(id: UserId): Result[Unit] =
      dbContext.run(StudentTable.filter(_.userId == lift(id)).delete).unit
  }

  lazy val live: ULayer[StudentRepository] =
    ZLayer.succeed(new ServiceImpl())
}
