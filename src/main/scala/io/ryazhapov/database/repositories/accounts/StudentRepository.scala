package io.ryazhapov.database.repositories.accounts

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.{Level, Student}
import zio.{Has, ULayer, ZLayer}

object StudentRepository extends Repository {

  import dbContext._

  type StudentRepository = Has[Service]

  private implicit val encodeLevel: MappedEncoding[Level, String] = MappedEncoding[Level, String](_.toString)
  private implicit val decodeLevel: MappedEncoding[String, Level] = MappedEncoding[String, Level](Level.fromString)
  lazy val live: ULayer[StudentRepository] =
    ZLayer.succeed(new ServiceImpl())

  trait Service {
    def create(student: Student): Result[Unit]

    def update(student: Student): Result[Unit]

    def get(id: UserId): Result[Option[Student]]

    def getAll: Result[List[Student]]

    def delete(id: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val StudentTable: Quoted[EntityQuery[Student]] = quote {
      querySchema[Student](""""Student"""")
    }

    override def create(student: Student): Result[Unit] =
      dbContext.run(StudentTable
        .insert(lift(student))
      ).unit

    override def update(student: Student): Result[Unit] =
      dbContext.run(StudentTable
        .update(lift(student))
      ).unit

    override def get(id: UserId): Result[Option[Student]] =
      dbContext.run(StudentTable
        .filter(_.userId == lift(id))
      ).map(_.headOption)

    override def getAll: Result[List[Student]] =
      dbContext.run(StudentTable)

    override def delete(id: UserId): Result[Unit] =
      dbContext.run(StudentTable
        .filter(_.userId == lift(id))
        .delete
      ).unit
  }
}
