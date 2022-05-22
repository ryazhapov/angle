package io.ryazhapov.database.repositories.billing

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.repositories.lessons.ScheduleRepository.ScheduleRepository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.billing.Replenishment
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object ReplenishmentRepository extends Repository {

  import dbContext._

  type ReplenishmentRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, ReplenishmentRepository] =
    ZLayer.fromService(new PostgresReplenishmentRepository(_))

  trait Service {
    def create(replenishment: Replenishment): Task[Unit]

    def getAll: Task[List[Replenishment]]

    def getByStudent(studentId: UserId): Task[List[Replenishment]]
  }

  class PostgresReplenishmentRepository(xa: Transactor[Task]) extends Service {

    lazy val replenishmentTable = quote(querySchema[Replenishment](""""Replenishment""""))

    override def create(replenishment: Replenishment): Task[Unit] =
      dbContext.run {
        replenishmentTable
          .insert(lift(replenishment))
      }.unit.transact(xa)

    override def getAll: Task[List[Replenishment]] =
      dbContext.run(replenishmentTable).transact(xa)

    override def getByStudent(studentId: UserId): Task[List[Replenishment]] =
      dbContext.run {
        replenishmentTable
          .filter(_.studentId == lift(studentId))
      }.transact(xa)
  }
}