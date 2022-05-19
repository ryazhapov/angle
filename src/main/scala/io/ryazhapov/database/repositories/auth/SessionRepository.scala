package io.ryazhapov.database.repositories.auth

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.{SessionId, UserId, auth}
import zio.{Has, ULayer, ZLayer}

object SessionRepository extends Repository {

  import dbContext._

  type SessionRepository = Has[Service]
  lazy val live: ULayer[SessionRepository.SessionRepository] =
    ZLayer.succeed(new ServiceImpl())

  trait Service {
    def insert(session: auth.Session): Result[Unit]

    def get(id: SessionId): Result[Option[auth.Session]]

    def getByUser(userId: UserId): Result[List[auth.Session]]

    def delete(id: SessionId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val sessionTable: Quoted[EntityQuery[auth.Session]] = quote {
      querySchema[auth.Session](""""Session"""")
    }

    override def insert(session: auth.Session): Result[Unit] =
      dbContext.run(sessionTable
        .insert(lift(session))
      ).unit

    override def get(id: SessionId): Result[Option[auth.Session]] =
      dbContext.run(sessionTable
        .filter(_.id == lift(id))
      ).map(_.headOption)

    override def getByUser(userId: UserId): Result[List[auth.Session]] =
      dbContext.run(sessionTable
        .filter(_.userId == lift(userId))
      )

    override def delete(id: SessionId): Result[Unit] =
      dbContext.run(sessionTable
        .filter(_.id == lift(id))
        .delete
      ).unit
  }
}
