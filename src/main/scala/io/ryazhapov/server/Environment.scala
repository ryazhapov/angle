package io.ryazhapov.server

import io.ryazhapov.config.ConfigService
import io.ryazhapov.config.ConfigService.Configuration
import io.ryazhapov.database.repositories.accounts.AdminRepository.AdminRepository
import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.database.repositories.accounts.TeacherRepository.TeacherRepository
import io.ryazhapov.database.repositories.accounts.{AdminRepository, StudentRepository, TeacherRepository}
import io.ryazhapov.database.repositories.auth.UserRepository.UserRepository
import io.ryazhapov.database.repositories.auth.{SessionRepository, UserRepository}
import io.ryazhapov.database.services.MigrationService.{Liqui, MigrationService}
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.database.services.{MigrationService, TransactorService}
import io.ryazhapov.logging.LoggerService
import io.ryazhapov.logging.LoggerService.LoggerService
import io.ryazhapov.services.accounts.AdminService.AdminService
import io.ryazhapov.services.accounts.StudentService.StudentService
import io.ryazhapov.services.accounts.TeacherService.TeacherService
import io.ryazhapov.services.accounts.{AdminService, StudentService, TeacherService}
import io.ryazhapov.services.auth.UserService
import io.ryazhapov.services.auth.UserService.UserService
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.random.Random

trait Environment {

  type AppEnvironment = Configuration with Clock with Blocking with Random with Console with
    Liqui with MigrationService with DBTransactor with LoggerService with
    UserService with UserRepository with
    TeacherRepository with TeacherService with
    AdminRepository with AdminService with
    StudentRepository with StudentService


  val appEnvironment =
    LoggerService.live >+> ConfigService.live >+> Blocking.live >+> Clock.live >+>
      TransactorService.live >+>
      SessionRepository.live >+>
      MigrationService.liquibaseLayer >+> MigrationService.live >+>
      UserRepository.live >+> UserService.live >+>
      TeacherRepository.live >+> TeacherService.live >+>
      AdminRepository.live >+> AdminService.live >+>
      StudentRepository.live >+> StudentService.live
}
