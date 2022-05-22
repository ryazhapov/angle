package io.ryazhapov.database.repositories.accounts

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Admin
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object AdminRepository extends Repository {

  import dbContext._

  type AdminRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, AdminRepository] =
    ZLayer.fromService(new PostgresAdminRepository(_))

  trait Service {
    def create(admin: Admin): Task[Unit]

    def update(admin: Admin): Task[Unit]

    def get(id: UserId): Task[Option[Admin]]

    def getAll: Task[List[Admin]]

    def delete(id: UserId): Task[Unit]
  }

  class PostgresAdminRepository(xa: Transactor[Task]) extends Service {

    lazy val adminTable = quote(querySchema[Admin](""""Admin""""))

    override def create(admin: Admin): Task[Unit] =
      dbContext.run {
        adminTable
          .insert(lift(admin))
      }.unit.transact(xa)

    override def update(admin: Admin): Task[Unit] =
      dbContext.run {
        adminTable
          .filter(_.userId == lift(admin.userId))
          .update(lift(admin))
      }.unit.transact(xa)

    override def get(id: UserId): Task[Option[Admin]] =
      dbContext.run {
        adminTable
          .filter(_.userId == lift(id))
      }.map(_.headOption).transact(xa)

    override def getAll: Task[List[Admin]] =
      dbContext.run(adminTable).transact(xa)

    override def delete(id: UserId): Task[Unit] =
      dbContext.run {
        adminTable
          .filter(_.userId == lift(id))
          .delete
      }.unit.transact(xa)
  }
}