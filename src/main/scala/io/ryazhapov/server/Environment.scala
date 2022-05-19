package io.ryazhapov.server

import io.ryazhapov.config.ConfigService
import io.ryazhapov.config.ConfigService.Configuration
import io.ryazhapov.database.repositories.accounts.AdminRepository.AdminRepository
import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.database.repositories.accounts.TeacherRepository.TeacherRepository
import io.ryazhapov.database.repositories.accounts.{AdminRepository, StudentRepository, TeacherRepository}
import io.ryazhapov.database.repositories.auth.UserRepository.UserRepository
import io.ryazhapov.database.repositories.auth.{SessionRepository, UserRepository}
import io.ryazhapov.database.repositories.billing.PaymentRepository.PaymentRepository
import io.ryazhapov.database.repositories.billing.ReplenishmentRepository.ReplenishmentRepository
import io.ryazhapov.database.repositories.billing.WithdrawalRepository.WithdrawalRepository
import io.ryazhapov.database.repositories.billing.{PaymentRepository, ReplenishmentRepository, WithdrawalRepository}
import io.ryazhapov.database.repositories.lessons.LessonRepository.LessonRepository
import io.ryazhapov.database.repositories.lessons.ScheduleRepository.ScheduleRepository
import io.ryazhapov.database.repositories.lessons.{LessonRepository, ScheduleRepository}
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
import io.ryazhapov.services.billing.PaymentService.PaymentService
import io.ryazhapov.services.billing.ReplenishmentService.ReplenishmentService
import io.ryazhapov.services.billing.WithdrawalService.WithdrawalService
import io.ryazhapov.services.billing.{PaymentService, ReplenishmentService, WithdrawalService}
import io.ryazhapov.services.lessons.LessonService.LessonService
import io.ryazhapov.services.lessons.ScheduleService.ScheduleService
import io.ryazhapov.services.lessons.{LessonService, ScheduleService}
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
    LessonRepository with LessonService with
    StudentRepository with StudentService with
    ReplenishmentRepository with ReplenishmentService with
    WithdrawalRepository with WithdrawalService with
    PaymentRepository with PaymentService with
    ScheduleRepository with ScheduleService


  val appEnvironment =
    LoggerService.live >+> ConfigService.live >+> Blocking.live >+> Clock.live >+>
      TransactorService.live >+>
      SessionRepository.live >+>
      MigrationService.liquibaseLayer >+> MigrationService.live >+>
      UserRepository.live >+> UserService.live >+>
      TeacherRepository.live >+> TeacherService.live >+>
      AdminRepository.live >+> AdminService.live >+>
      LessonRepository.live >+> LessonService.live >+>
      StudentRepository.live >+> StudentService.live >+>
      ReplenishmentRepository.live >+> ReplenishmentService.live >+>
      PaymentRepository.live >+> PaymentService.live >+>
      WithdrawalRepository.live >+> WithdrawalService.live >+>
      ScheduleRepository.live >+> ScheduleService.live
}
