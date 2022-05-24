package io.ryazhapov.database.repositories.billing

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.accounts.Student
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.billing.Replenishment
import io.ryazhapov.domain.{ReplenishmentId, UserId}
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object ReplenishmentRepository extends Repository {

  import dbContext._

  type ReplenishmentRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, ReplenishmentRepository] =
    ZLayer.fromService(new PostgresReplenishmentRepository(_))

  trait Service {
    def create(user: User, student: Student, replenishment: Replenishment): Task[ReplenishmentId]

    def getAll: Task[List[Replenishment]]

    def getByStudent(studentId: UserId): Task[List[Replenishment]]
  }

  class PostgresReplenishmentRepository(xa: Transactor[Task]) extends Service {

    lazy val replenishmentTable = quote(querySchema[Replenishment](""""Replenishment""""))
    lazy val studentTable = quote(querySchema[Student](""""Student""""))
    lazy val userTable = quote(querySchema[User](""""User""""))

    override def create(user: User, student: Student, replenishment: Replenishment): Task[ReplenishmentId] = {

      val updateUser = dbContext.run {
        userTable
          .filter(_.id == lift(user.id))
          .update(lift(user))
      }.unit

      val updateStudent = dbContext.run {
        studentTable
          .filter(_.userId == lift(student.userId))
          .update(lift(student))
      }.unit

      val createReplenishment = dbContext.run {
        replenishmentTable
          .insert(lift(replenishment))
          .returningGenerated(_.id)
      }

      val transaction = for {
        _ <- updateUser
        _ <- updateStudent
        replenishmentId <- createReplenishment
      } yield replenishmentId

      transaction.transact(xa)
    }

    override def getAll: Task[List[Replenishment]] =
      dbContext.run(replenishmentTable).transact(xa)

    override def getByStudent(studentId: UserId): Task[List[Replenishment]] =
      dbContext.run {
        replenishmentTable
          .filter(_.studentId == lift(studentId))
      }.transact(xa)
  }
}