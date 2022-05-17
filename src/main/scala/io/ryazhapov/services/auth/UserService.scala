package io.ryazhapov.services.auth

import io.ryazhapov.database.repositories.auth.SessionRepository.SessionRepository
import io.ryazhapov.database.repositories.auth.UserRepository.UserRepository
import io.ryazhapov.database.repositories.auth.{SessionRepository, UserRepository}
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.auth.{Session, User}
import io.ryazhapov.domain.{Password, SessionId, UserId}
import io.ryazhapov.errors.{IncorrectUserPassword, SessionNotFound, UserNotExist, UserNotFound}
import io.ryazhapov.utils.SecurityUtils
import zio.interop.catz._
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, RLayer, ZIO, ZLayer}

import java.util.UUID

@accessible
object UserService {

  type UserService = Has[Service]

  trait Service {
    def createUser(user: User): RIO[DBTransactor, Unit]

    def updateUser(user: User): RIO[DBTransactor, User]

    def verifyUser(id: UserId): RIO[DBTransactor, Unit]

    def getUser(id: UserId): RIO[DBTransactor, User]

    def getAllUsers: RIO[DBTransactor, List[User]]

    def getUnverifiedUsers: RIO[DBTransactor, List[User]]

    def userExists(email: String): RIO[DBTransactor, Boolean]

    def validateUser(email: String, password: Password): RIO[DBTransactor, User]

    def deleteUser(id: UserId): RIO[DBTransactor, Unit]

    def deleteUserByEmail(email: String): RIO[DBTransactor, Unit]

    def getSession(sessionId: String): RIO[DBTransactor, Session]

    def getSessions(userId: UserId): RIO[DBTransactor, List[Session]]

    def addSession(userId: UserId): RIO[DBTransactor with Random, SessionId]

    def deleteSession(id: SessionId): RIO[DBTransactor, Unit]
  }

  class ServiceImpl(
    usersRepository: UserRepository.Service,
    sessionsRepository: SessionRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createUser(user: User): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.create(user).transact(transactor)
      } yield ()

    //TODO
    override def updateUser(user: User): RIO[DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.update(user).transact(transactor)
      } yield user

    override def verifyUser(id: UserId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.verify(id).transact(transactor)
      } yield ()

    override def getUser(id: UserId): RIO[DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        userOpt <- usersRepository.get(id).transact(transactor)
        user <- ZIO.fromEither(userOpt.toRight(UserNotFound))
      } yield user

    override def getAllUsers: RIO[DBTransactor, List[User]] =
      for {
        transactor <- TransactorService.databaseTransactor
        users <- usersRepository.getAll.transact(transactor)
      } yield users

    override def getUnverifiedUsers: RIO[DBTransactor, List[User]] =
      for {
        transactor <- TransactorService.databaseTransactor
        users <- usersRepository.getUnverified.transact(transactor)
      } yield users

    override def userExists(email: String): RIO[DBTransactor, Boolean] =
      for {
        transactor <- TransactorService.databaseTransactor
        userOpt <- usersRepository.getByEmail(email).transact(transactor)
        result = userOpt match {
          case None    => false
          case Some(_) => true
        }
      } yield result

    override def validateUser(email: String, password: Password): RIO[DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        userOpt <- usersRepository.getByEmail(email).transact(transactor)
        user <- ZIO.fromEither(userOpt.toRight(UserNotExist(email)))
        isPasswordCorrect <- ZIO.effect(SecurityUtils.checkSecret(password, user.salt, user.passwordHash))
        user <- ZIO.cond(isPasswordCorrect, user, IncorrectUserPassword)
      } yield user

    //TODO
    override def deleteUser(id: UserId): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.delete(id).transact(transactor)
      } yield ()

    //TODO
    override def deleteUserByEmail(email: Password): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.deleteByEmail(email).transact(transactor)
      } yield ()

    override def getSession(sessionId: String): RIO[DBTransactor, Session] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(sessionId))
        sessionOpt <- sessionsRepository.get(id).transact(transactor)
        session <- ZIO.fromEither(sessionOpt.toRight(SessionNotFound))
      } yield session

    //TODO
    override def getSessions(userId: UserId): RIO[DBTransactor, List[Session]] =
      for {
        transactor <- TransactorService.databaseTransactor
        sessions <- sessionsRepository.getByUser(userId).transact(transactor)
      } yield sessions

    override def addSession(userId: UserId): RIO[DBTransactor with Random, SessionId] =
      for {
        transactor <- TransactorService.databaseTransactor
        uuid <- zio.random.nextUUID
        _ <- sessionsRepository.insert(Session(uuid, userId)).transact(transactor)
      } yield uuid

    override def deleteSession(id: SessionId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- sessionsRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: RLayer[UserRepository with SessionRepository, UserService] =
    ZLayer.fromServices[UserRepository.Service, SessionRepository.Service, UserService.Service] {
      (usersRepo, sessionsRepo) => new ServiceImpl(usersRepo, sessionsRepo)
    }
}
