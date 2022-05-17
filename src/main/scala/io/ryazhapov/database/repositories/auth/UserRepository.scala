package io.ryazhapov.database.repositories.auth

import io.ryazhapov.database.repositories.{Repository, auth}
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Role
import io.ryazhapov.domain.auth.User
import zio.{Has, ULayer, ZLayer}

object UserRepository extends Repository {

  import dbContext._

  type UserRepository = Has[Service]

  private implicit val encodeRole: MappedEncoding[Role, String] = MappedEncoding[Role, String](_.toString)
  private implicit val decodeRole: MappedEncoding[String, Role] = MappedEncoding[String, Role](Role.fromString)

  trait Service {
    def create(user: User): Result[Unit]

    def update(user: User): Result[Unit]

    def get(id: UserId): Result[Option[User]]

    def verify(id: UserId): Result[Unit]

    def getAll: Result[List[User]]

    def getUnverified: Result[List[User]]

    def getByEmail(email: String): Result[Option[User]]

    def delete(id: UserId): Result[Unit]

    def deleteByEmail(email: String): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val userTable: Quoted[EntityQuery[User]] = quote {
      querySchema[User](""""User"""")
    }

    override def create(user: User): Result[Unit] =
      dbContext.run(userTable.insert(lift(user))).unit

    override def update(user: User): Result[Unit] =
      dbContext.run(userTable.update(lift(user))).unit

    override def verify(id: UserId): Result[Unit] =
      dbContext.run(userTable.filter(_.id == lift(id)).update(_.isVerified -> lift(true))).unit

    override def get(id: UserId): Result[Option[User]] =
      dbContext.run(userTable.filter(_.id == lift(id))).map(_.headOption)

    override def getAll: Result[List[User]] =
      dbContext.run(userTable)

    override def getUnverified: Result[List[User]] =
      dbContext.run(userTable.filter(_.isVerified == lift(false)))

    override def getByEmail(email: String): Result[Option[User]] =
      dbContext.run(userTable.filter(_.email == lift(email))).map(_.headOption)

    override def delete(id: UserId): Result[Unit] =
      dbContext.run(userTable.filter(_.id == lift(id)).delete).unit

    override def deleteByEmail(email: String): Result[Unit] =
      dbContext.run(userTable.filter(_.email == lift(email)).delete).unit
  }

  lazy val live: ULayer[UserRepository.UserRepository] =
    ZLayer.succeed(new ServiceImpl())
}
