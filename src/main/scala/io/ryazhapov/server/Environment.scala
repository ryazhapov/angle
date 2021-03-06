package io.ryazhapov.server

import io.ryazhapov.config.ConfigService
import io.ryazhapov.config.ConfigService.Configuration
import io.ryazhapov.database.repositories.accounts.AdminRepository.AdminRepository
import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.database.repositories.accounts.TeacherRepository.TeacherRepository
import io.ryazhapov.database.repositories.accounts.{AdminRepository, StudentRepository, TeacherRepository}
import io.ryazhapov.database.repositories.auth.SessionRepository.SessionRepository
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
import zio.magic._
import zio.{ZEnv, ZLayer}

trait Environment {

  type AppEnvironment = ZEnv with Services with Blocking

  type Services = AdminRepository with
    AdminService with
    Configuration with
    DBTransactor with
    LessonRepository with
    LessonService with
    Liqui with
    LoggerService with
    MigrationService with
    PaymentRepository with
    PaymentService with
    ReplenishmentRepository with
    ReplenishmentService with
    ScheduleRepository with
    ScheduleService with
    SessionRepository with
    StudentRepository with
    StudentService with
    TeacherRepository with
    TeacherService with
    UserRepository with
    UserService with
    WithdrawalRepository with
    WithdrawalService

  val appEnvironment = ZLayer.wireSome[ZEnv, Services](
    AdminRepository.live,
    AdminService.live,
    ConfigService.live,
    LessonRepository.live,
    LessonService.live,
    LoggerService.live,
    MigrationService.liquibaseLayer,
    MigrationService.live,
    PaymentRepository.live,
    PaymentService.live,
    ReplenishmentRepository.live,
    ReplenishmentService.live,
    ScheduleRepository.live,
    ScheduleService.live,
    SessionRepository.live,
    StudentRepository.live,
    StudentService.live,
    TeacherRepository.live,
    TeacherService.live,
    TransactorService.live,
    UserRepository.live,
    UserService.live,
    WithdrawalRepository.live,
    WithdrawalService.live
  )
}