package io.ryazhapov.database.repositories.accounts

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.{Admin, Level}
import zio.{Has, ULayer, ZLayer}

object AdminRepository extends Repository {

  import dbContext._

  type AdminRepository = Has[Service]

  private implicit val encodeLevel: MappedEncoding[Level, String] = MappedEncoding[Level, String](_.toString)
  private implicit val decodeLevel: MappedEncoding[String, Level] = MappedEncoding[String, Level](Level.fromString)

  trait Service {
    def create(admin: Admin): Result[Unit]

    def update(admin: Admin): Result[Unit]

    def get(id: UserId): Result[Option[Admin]]

    def getAll: Result[List[Admin]]

    def delete(id: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val adminTable: Quoted[EntityQuery[Admin]] = quote {
      querySchema[Admin](""""Admin"""")
    }

    override def create(admin: Admin): Result[Unit] =
      dbContext.run(adminTable.insert(lift(admin))).unit

    override def update(admin: Admin): Result[Unit] =
      dbContext.run(adminTable.filter(_.userId == lift(admin.userId)).update(lift(admin))).unit

    override def get(id: UserId): Result[Option[Admin]] =
      dbContext.run(adminTable.filter(_.userId == lift(id))).map(_.headOption)

    override def getAll: Result[List[Admin]] =
      dbContext.run(adminTable)

    override def delete(id: UserId): Result[Unit] =
      dbContext.run(adminTable.filter(_.userId == lift(id)).delete).unit
  }

  lazy val live: ULayer[AdminRepository] =
    ZLayer.succeed(new ServiceImpl())
}
