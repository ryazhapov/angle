package io.ryazhapov.services.auth

import io.ryazhapov.database.repositories.accounts.{AdminRepository, StudentRepository, TeacherRepository}
import io.ryazhapov.database.repositories.auth.{SessionRepository, UserRepository}
import io.ryazhapov.domain.accounts.Role.{AdminRole, StudentRole, TeacherRole}
import io.ryazhapov.domain.accounts.{Admin, Student, Teacher}
import io.ryazhapov.domain.auth.{Session, SignUpRequest, User}
import io.ryazhapov.domain.{Password, SessionId, UserId}
import io.ryazhapov.errors._
import io.ryazhapov.utils.SecurityUtils
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object UserService {

  type UserService = Has[Service]
  lazy val live = ZLayer.fromServices[
    UserRepository.Service,
    SessionRepository.Service,
    TeacherRepository.Service,
    AdminRepository.Service,
    StudentRepository.Service,
    UserService.Service
  ]((userRepo, sessionRepo, teacherRepo, adminRepo, studentRepo) =>
    new ServiceImpl(userRepo, sessionRepo, teacherRepo, adminRepo, studentRepo))

  trait Service {
    def createUser(request: SignUpRequest): Task[UserId]

    def updateUser(user: User): Task[Unit]

    def verifyUser(id: UserId): Task[Unit]

    def getUser(id: UserId): Task[User]

    def getAllUsers: Task[List[User]]

    def getUnverifiedUsers: Task[List[User]]

    def userExists(email: String): Task[Boolean]

    def validateUser(email: String, password: Password): Task[User]

    def deleteUser(id: UserId): Task[Unit]

    def deleteUserByEmail(email: String): Task[Unit]

    def addSession(userId: UserId): Task[SessionId]

    def getSession(sessionId: String): Task[Session]

    def getSessions(userId: UserId): Task[List[Session]]

    def deleteSession(id: SessionId): Task[Unit]
  }

  class ServiceImpl(
    usersRepository: UserRepository.Service,
    sessionsRepository: SessionRepository.Service,
    teacherRepository: TeacherRepository.Service,
    adminRepository: AdminRepository.Service,
    studentRepository: StudentRepository.Service
  ) extends Service {

    override def createUser(request: SignUpRequest): Task[UserId] =
      for {
        userOpt <- usersRepository.getByEmail(request.email)
        _ <- userOpt match {
          case None    => ZIO.succeed(())
          case Some(_) => ZIO.fail(UserAlreadyExists)
        }
        salt = SecurityUtils.generateSalt()
        user = User(
          0,
          request.email,
          request.role,
          if (request.role == AdminRole) true else false,
          SecurityUtils.countSecretHash(request.password, salt),
          salt
        )
        id <- usersRepository.create(user)
        _ <- request.role match {
          case AdminRole   => adminRepository.create(Admin(userId = id))
          case TeacherRole => teacherRepository.create(Teacher(userId = id))
          case StudentRole => studentRepository.create(Student(userId = id))
        }
      } yield id

    //TODO
    override def updateUser(user: User): Task[Unit] =
      usersRepository.update(user)

    override def verifyUser(id: UserId): Task[Unit] =
      usersRepository.verify(id)

    override def getUser(id: UserId): Task[User] =
      for {
        userOpt <- usersRepository.get(id)
        user <- ZIO.fromEither(userOpt.toRight(UserNotFound))
      } yield user

    override def getAllUsers: Task[List[User]] =
      usersRepository.getAll

    override def getUnverifiedUsers: Task[List[User]] =
      usersRepository.getUnverified

    override def userExists(email: String): Task[Boolean] =
      for {
        userOpt <- usersRepository.getByEmail(email)
        result = userOpt match {
          case None    => false
          case Some(_) => true
        }
      } yield result

    override def validateUser(email: String, password: Password): Task[User] =
      for {
        userOpt <- usersRepository.getByEmail(email)
        user <- ZIO.fromEither(userOpt.toRight(UserNotExist(email)))
        isPasswordCorrect <- ZIO.effect(SecurityUtils.checkSecret(password, user.salt, user.passwordHash))
        user <- ZIO.cond(isPasswordCorrect, user, IncorrectUserPassword)
      } yield user

    //TODO
    override def deleteUser(id: UserId): Task[Unit] =
      usersRepository.delete(id)

    //TODO
    override def deleteUserByEmail(email: Password): Task[Unit] =
      usersRepository.deleteByEmail(email)

    override def addSession(userId: UserId): Task[SessionId] =
      sessionsRepository.insert(Session(0, userId))

    override def getSession(sessionId: String): Task[Session] =
      for {
        sessionOpt <- sessionsRepository.get(Integer.parseInt(sessionId))
        session <- ZIO.fromEither(sessionOpt.toRight(SessionNotFound))
      } yield session

    //TODO
    override def getSessions(userId: UserId): Task[List[Session]] =
      sessionsRepository.getByUser(userId)

    override def deleteSession(id: SessionId): Task[Unit] =
      sessionsRepository.delete(id)
  }
}
