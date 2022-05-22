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
import zio.{Has, RIO, Task, ZIO, ZLayer}

@accessible
object AdminService {

  type AdminService = Has[Service]

  lazy val live = ZLayer.fromService[
    AdminRepository.Service,
    AdminService.Service
  ](repo => new ServiceImpl(repo))

  trait Service {
    def createAdmin(admin: Admin): Task[Unit]

    def updateAdmin(admin: Admin): Task[Unit]

    def getAdmin(id: UserId): Task[Admin]

    def getAllAdmins: Task[List[Admin]]

    def deleteAdmin(id: UserId): Task[Unit]
  }

  class ServiceImpl(
    adminRepository: AdminRepository.Service
  ) extends Service {

    override def createAdmin(admin: Admin): Task[Unit] =
      adminRepository.create(admin)

    override def updateAdmin(admin: Admin): Task[Unit] =
      adminRepository.update(admin)

    override def getAdmin(id: UserId): Task[Admin] =
      for {
        adminOpt <- adminRepository.get(id)
        admin <- ZIO.fromEither(adminOpt.toRight(AdminNotFound))
      } yield admin

    override def getAllAdmins: Task[List[Admin]] =
      adminRepository.getAll

    //TODO
    override def deleteAdmin(id: UserId): Task[Unit] =
      adminRepository.delete(id)
  }
}
