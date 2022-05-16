package io.ryazhapov.services.auth

import io.ryazhapov.database.dao.auth
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
    def createUser(user: User, password: Password): RIO[DBTransactor, Unit]

    def updateUser(user: User, password: Password): RIO[DBTransactor, User]

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

    override def createUser(user: User, password: Password): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao = user.toDAO(password)
        _ <- usersRepository.create(userDao).transact(transactor)
      } yield ()

    override def updateUser(user: User, password: Password): RIO[DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao = user.toDAO(password)
        _ <- usersRepository.update(userDao).transact(transactor)
      } yield user

    override def verifyUser(id: UserId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.verify(id).transact(transactor)
      } yield ()

    override def getUser(id: UserId): RIO[DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao <- usersRepository.get(id).transact(transactor)
        user <- ZIO.fromEither(userDao.map(_.toUser).toRight(UserNotFound))
      } yield user

    override def getAllUsers: RIO[DBTransactor, List[User]] =
      for {
        transactor <- TransactorService.databaseTransactor
        usersDao <- usersRepository.getAll.transact(transactor)
        users = usersDao.map(_.toUser)
      } yield users

    override def getUnverifiedUsers: RIO[DBTransactor, List[User]] =
      for {
        transactor <- TransactorService.databaseTransactor
        usersDao <- usersRepository.getUnverified.transact(transactor)
        users = usersDao.map(_.toUser)
      } yield users

    override def userExists(email: String): RIO[DBTransactor, Boolean] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao <- usersRepository.getByEmail(email).transact(transactor)
        result = userDao match {
          case None    => false
          case Some(_) => true
        }
      } yield result

    override def validateUser(email: String, password: Password): RIO[DBTransactor, User] =
      for {
        transactor <- TransactorService.databaseTransactor
        userDao <- usersRepository.getByEmail(email).transact(transactor)
        user <- ZIO.fromEither(userDao.toRight(UserNotExist(email)))
        isPasswordCorrect <- ZIO.effect(SecurityUtils.checkSecret(password, user.salt, user.passwordHash))
        user <- ZIO.cond(isPasswordCorrect, user.toUser, IncorrectUserPassword)
      } yield user

    override def deleteUser(id: UserId): RIO[TransactorService.DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.delete(id).transact(transactor)
      } yield ()

    override def deleteUserByEmail(email: Password): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- usersRepository.deleteByEmail(email).transact(transactor)
      } yield ()


    override def getSession(sessionId: String): RIO[DBTransactor, Session] =
      for {
        transactor <- TransactorService.databaseTransactor
        id <- ZIO.effect(UUID.fromString(sessionId))
        sessionDto <- sessionsRepository.get(id).transact(transactor)
        session <- ZIO.fromEither(sessionDto.map(_.toSession).toRight(SessionNotFound))
      } yield session

    override def getSessions(userId: UserId): RIO[DBTransactor, List[Session]] =
      for {
        transactor <- TransactorService.databaseTransactor
        sessionsDao <- sessionsRepository.getByUser(userId).transact(transactor)
        sessions = sessionsDao.map(_.toSession)
      } yield sessions

    override def addSession(userId: UserId): RIO[DBTransactor with Random, SessionId] =
      for {
        transactor <- TransactorService.databaseTransactor
        uuid <- zio.random.nextUUID
        _ <- sessionsRepository.insert(auth.SessionDao(uuid, userId)).transact(transactor)
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
