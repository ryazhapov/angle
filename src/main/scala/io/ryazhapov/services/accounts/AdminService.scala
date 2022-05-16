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

import java.util.UUID

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
        _ <- adminRepository.create(admin.toDao).transact(transactor).unit
      } yield ()

    override def updateAdmin(admin: Admin): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- adminRepository.update(admin.toDao).transact(transactor)
      } yield admin

    override def getAdmin(id: UserId): RIO[DBTransactor, Admin] =
      for {
        transactor <- TransactorService.databaseTransactor
        adminDao <- adminRepository.get(id).transact(transactor)
        admin <- ZIO.fromEither(adminDao.map(_.toAdmin).toRight(AdminNotFound))
      } yield admin

    override def getAllAdmins: RIO[DBTransactor, List[Admin]] =
      for {
        transactor <- TransactorService.databaseTransactor
        adminsDao <- adminRepository.getAll.transact(transactor)
        admins = adminsDao.map(_.toAdmin)
      } yield admins

    override def deleteAdmin(id: UserId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- adminRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[AdminRepository, Nothing, AdminService] =
    ZLayer.fromService[AdminRepository.Service, AdminService.Service](repo => new ServiceImpl(repo))
}
