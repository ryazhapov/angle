package io.ryazhapov.services.accounts

import io.ryazhapov.database.repositories.accounts.AdminRepository
import io.ryazhapov.database.repositories.accounts.AdminRepository.AdminRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Admin
import io.ryazhapov.errors.AdminNotFound
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZIO, ZLayer}

@accessible
object AdminService {

  type AdminService = Has[Service]

  trait Service {
    def createAdmin(admin: Admin): RIO[DBTransactor, Unit]

    def updateAdmin(admin: Admin): RIO[DBTransactor, Unit]

    def getAdmin(id: UserId): RIO[DBTransactor, Admin]

    def getAllAdmins: RIO[DBTransactor, List[Admin]]

    def deleteAdmin(id: UserId): RIO[DBTransactor, Unit]
  }

  class ServiceImpl(
    adminRepository: AdminRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createAdmin(admin: Admin): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- adminRepository.create(admin).transact(transactor).unit
      } yield ()

    override def updateAdmin(admin: Admin): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- adminRepository.update(admin).transact(transactor).unit
      } yield ()

    override def getAdmin(id: UserId): RIO[DBTransactor, Admin] =
      for {
        transactor <- TransactorService.databaseTransactor
        adminOpt <- adminRepository.get(id).transact(transactor)
        admin <- ZIO.fromEither(adminOpt.toRight(AdminNotFound))
      } yield admin

    override def getAllAdmins: RIO[DBTransactor, List[Admin]] =
      for {
        transactor <- TransactorService.databaseTransactor
        admins <- adminRepository.getAll.transact(transactor)
      } yield admins

    //TODO
    override def deleteAdmin(id: UserId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- adminRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[AdminRepository, Nothing, AdminService] =
    ZLayer.fromService[AdminRepository.Service, AdminService.Service](repo => new ServiceImpl(repo))
}
