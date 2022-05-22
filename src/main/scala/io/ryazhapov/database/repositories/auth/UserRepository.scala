package io.ryazhapov.database.repositories.auth

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.auth.User
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object UserRepository extends Repository {

  import dbContext._

  type UserRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, UserRepository] =
    ZLayer.fromService(new PostgresUserRepository(_))

  trait Service {
    def create(user: User): Task[UserId]

    def update(user: User): Task[Unit]

    def get(id: UserId): Task[Option[User]]

    def verify(id: UserId): Task[Unit]

    def getAll: Task[List[User]]

    def getUnverified: Task[List[User]]

    def getByEmail(email: String): Task[Option[User]]

    def delete(id: UserId): Task[Unit]

    def deleteByEmail(email: String): Task[Unit]
  }

  class PostgresUserRepository(xa: Transactor[Task]) extends Service {
    lazy val userTable = quote(querySchema[User](""""User""""))

    override def create(user: User): Task[UserId] =
      dbContext.run {
        userTable
          .insert(lift(user))
          .returningGenerated(_.id)
      }.transact(xa)

    override def update(user: User): Task[Unit] =
      dbContext.run {
        userTable
          .filter(_.id == lift(user.id))
          .update(lift(user))
      }.unit.transact(xa)

    override def verify(id: UserId): Task[Unit] =
      dbContext.run {
        userTable
          .filter(_.id == lift(id))
          .update(_.verified -> lift(true))
      }.unit.transact(xa)

    override def get(id: UserId): Task[Option[User]] =
      dbContext.run {
        userTable
          .filter(_.id == lift(id))
      }.map(_.headOption).transact(xa)

    override def getAll: Task[List[User]] =
      dbContext.run(userTable)
        .transact(xa)

    override def getUnverified: Task[List[User]] =
      dbContext.run {
        userTable
          .filter(_.verified == lift(false))
      }.transact(xa)

    override def getByEmail(email: String): Task[Option[User]] =
      dbContext.run {
        userTable
          .filter(_.email == lift(email))
      }.map(_.headOption).transact(xa)

    override def delete(id: UserId): Task[Unit] =
      dbContext.run {
        userTable
          .filter(_.id == lift(id))
          .delete
      }.unit.transact(xa)

    override def deleteByEmail(email: String): Task[Unit] =
      dbContext.run {
        userTable
          .filter(_.email == lift(email))
          .delete
      }.unit.transact(xa)
  }
}