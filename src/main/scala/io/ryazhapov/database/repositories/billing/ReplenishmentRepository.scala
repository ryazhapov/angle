package io.ryazhapov.database.repositories.billing

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.billing.Replenishment
import zio.{Has, ULayer, ZLayer}

object ReplenishmentRepository extends Repository {

  import dbContext._

  type ReplenishmentRepository = Has[Service]
  lazy val live: ULayer[ReplenishmentRepository] =
    ZLayer.succeed(new ServiceImpl())

  trait Service {
    def create(replenishment: Replenishment): Result[Unit]

    def getAll: Result[List[Replenishment]]

    def getByStudent(studentId: UserId): Result[List[Replenishment]]
  }

  class ServiceImpl() extends Service {
    lazy val replenishmentTable = quote {
      querySchema[Replenishment](""""Replenishment"""")
    }

    override def create(replenishment: Replenishment): Result[Unit] =
      dbContext.run(replenishmentTable
        .insert(lift(replenishment))
      ).unit

    override def getAll: Result[List[Replenishment]] =
      dbContext.run(replenishmentTable)

    override def getByStudent(studentId: UserId): Result[List[Replenishment]] =
      dbContext.run(replenishmentTable
        .filter(_.studentId == lift(studentId))
      )
  }
}