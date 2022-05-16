package io.ryazhapov.database.repositories.auth

import io.ryazhapov.database.dao.auth.SessionDao
import io.ryazhapov.database.repositories.{Repository, auth}
import io.ryazhapov.domain.{SessionId, UserId}
import zio.{Has, ULayer, ZLayer}

object SessionRepository extends Repository {

  import dbContext._

  type SessionRepository = Has[Service]

  trait Service {
    def insert(session: SessionDao): Result[Unit]

    def get(id: SessionId): Result[Option[SessionDao]]

    def getByUser(userId: UserId): Result[List[SessionDao]]

    def delete(id: SessionId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val sessionTable: Quoted[EntityQuery[SessionDao]] = quote {
      querySchema[SessionDao](""""Session"""")
    }

    override def insert(session: SessionDao): Result[Unit] =
      dbContext.run(sessionTable.insert(lift(session))).unit

    override def get(id: SessionId): Result[Option[SessionDao]] =
      dbContext.run(sessionTable.filter(_.id == lift(id))).map(_.headOption)

    override def getByUser(userId: UserId): Result[List[SessionDao]] =
      dbContext.run(sessionTable.filter(_.userId == lift(userId)))

    override def delete(id: SessionId): Result[Unit] =
      dbContext.run(sessionTable.filter(_.id == lift(id)).delete).unit
  }

  lazy val live: ULayer[SessionRepository.SessionRepository] =
    ZLayer.succeed(new ServiceImpl())
}
