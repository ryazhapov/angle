package io.ryazhapov.database.repositories.auth

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.{SessionId, UserId, auth}
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object SessionRepository extends Repository {

  import dbContext._

  type SessionRepository = Has[Service]

  val live: URLayer[DBTransactor, SessionRepository] =
    ZLayer.fromService(new PostgresSessionRepository(_))

  trait Service {
    def insert(session: auth.Session): Task[SessionId]

    def get(id: SessionId): Task[Option[auth.Session]]

    def getByUser(userId: UserId): Task[List[auth.Session]]

    def delete(id: SessionId): Task[Unit]
  }

  class PostgresSessionRepository(xa: Transactor[Task]) extends Service {

    lazy val sessionTable = quote(querySchema[auth.Session](""""Session""""))

    override def insert(session: auth.Session): Task[SessionId] =
      dbContext.run {
        sessionTable
          .insert(lift(session))
          .returningGenerated(_.id)
      }.transact(xa)

    override def get(id: SessionId): Task[Option[auth.Session]] =
      dbContext.run {
        sessionTable
          .filter(_.id == lift(id))
      }.map(_.headOption).transact(xa)

    override def getByUser(userId: UserId): Task[List[auth.Session]] =
      dbContext.run {
        sessionTable
          .filter(_.userId == lift(userId))
      }.transact(xa)

    override def delete(id: SessionId): Task[Unit] =
      dbContext.run {
        sessionTable
          .filter(_.id == lift(id))
          .delete
      }.unit.transact(xa)
  }
}