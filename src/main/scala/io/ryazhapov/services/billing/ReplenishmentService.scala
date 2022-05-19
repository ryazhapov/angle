package io.ryazhapov.services.billing

import io.ryazhapov.database.repositories.billing.ReplenishmentRepository
import io.ryazhapov.database.repositories.billing.ReplenishmentRepository.ReplenishmentRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.billing.Replenishment
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZLayer}

@accessible
object ReplenishmentService {

  type ReplenishmentService = Has[Service]
  lazy val live: ZLayer[ReplenishmentRepository, Nothing, ReplenishmentService] =
    ZLayer.fromService[ReplenishmentRepository.Service, ReplenishmentService.Service](repo => new ServiceImpl(repo))

  trait Service {
    def createReplenishment(replenishment: Replenishment): RIO[DBTransactor, Unit]

    def getAllReplenishments: RIO[DBTransactor, List[Replenishment]]

    def getStudentsReplenishment(studentId: UserId): RIO[DBTransactor, List[Replenishment]]
  }

  class ServiceImpl(
    replenishmentRepository: ReplenishmentRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createReplenishment(replenishment: Replenishment): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- replenishmentRepository.create(replenishment).transact(transactor).unit
      } yield ()

    override def getAllReplenishments: RIO[DBTransactor, List[Replenishment]] =
      for {
        transactor <- TransactorService.databaseTransactor
        replenishment <- replenishmentRepository.getAll.transact(transactor)
      } yield replenishment

    override def getStudentsReplenishment(studentId: UserId): RIO[DBTransactor, List[Replenishment]] =
      for {
        transactor <- TransactorService.databaseTransactor
        replenishment <- replenishmentRepository.getByStudent(studentId).transact(transactor)
      } yield replenishment
  }
}
