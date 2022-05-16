package io.ryazhapov.database.repositories.accounts

import io.ryazhapov.database.dao.accounts.AdminDao
import io.ryazhapov.database.repositories.{Repository, accounts}
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Level
import zio.{Has, ULayer, ZLayer}

object AdminRepository extends Repository {

  import dbContext._

  type AdminRepository = Has[Service]

  private implicit val encodeLevel: MappedEncoding[Level, String] = MappedEncoding[Level, String](_.toString)
  private implicit val decodeLevel: MappedEncoding[String, Level] = MappedEncoding[String, Level](Level.fromString)

  trait Service {
    def create(admin: AdminDao): Result[Unit]

    def update(admin: AdminDao): Result[Unit]

    def get(id: UserId): Result[Option[AdminDao]]

    def getAll: Result[List[AdminDao]]

    def delete(id: UserId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val adminTable: Quoted[EntityQuery[AdminDao]] = quote {
      querySchema[AdminDao](""""Admin"""")
    }

    override def create(admin: AdminDao): Result[Unit] =
      dbContext.run(adminTable.insert(lift(admin))).unit

    override def update(admin: AdminDao): Result[Unit] =
      dbContext.run(adminTable.filter(_.userId == lift(admin.userId)).update(lift(admin))).unit

    override def get(id: UserId): Result[Option[AdminDao]] =
      dbContext.run(adminTable.filter(_.userId == lift(id))).map(_.headOption)

    override def getAll: Result[List[AdminDao]] =
      dbContext.run(adminTable)

    override def delete(id: UserId): Result[Unit] =
      dbContext.run(adminTable.filter(_.userId == lift(id)).delete).unit
  }

  lazy val live: ULayer[AdminRepository] =
    ZLayer.succeed(new ServiceImpl())
}
