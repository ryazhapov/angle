package io.ryazhapov.services.lessons

import io.ryazhapov.database.repositories.lessons.ScheduleRepository
import io.ryazhapov.database.repositories.lessons.ScheduleRepository.ScheduleRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.lessions.Schedule
import io.ryazhapov.domain.{ScheduleId, UserId}
import io.ryazhapov.errors.{InvalidScheduleTime, ScheduleNotFound}
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZIO, ZLayer}

import java.time.ZonedDateTime

@accessible
object ScheduleService {
  type ScheduleService = Has[Service]

  trait Service {
    def createSchedule(schedule: Schedule): RIO[DBTransactor, Unit]

    def updateSchedule(schedule: Schedule): RIO[DBTransactor, Unit]

    def getSchedule(id: ScheduleId): RIO[DBTransactor, Schedule]

    def getAllSchedules: RIO[DBTransactor, List[Schedule]]

    def getScheduleByTeacherId(teacherId: UserId): RIO[DBTransactor, List[Schedule]]

    def getScheduleByTime(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): RIO[DBTransactor, List[Schedule]]

    def isOverlapping(schedule: Schedule): RIO[DBTransactor, Boolean]

    def deleteSchedule(id: ScheduleId): RIO[DBTransactor, Unit]
  }

  def isValidSchedule(schedule: Schedule): Boolean =
    schedule.startsAt.isBefore(schedule.endsAt) && !schedule.startsAt.isEqual(schedule.endsAt)

  class ServiceImpl(
    scheduleRepository: ScheduleRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createSchedule(schedule: Schedule): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- ZIO.cond(isValidSchedule(schedule), (), InvalidScheduleTime)
        _ <- scheduleRepository.create(schedule).transact(transactor).unit
      } yield ()

    override def updateSchedule(schedule: Schedule): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        updateSchedule = scheduleRepository.update(schedule).transact(transactor)
        _ <- ZIO.cond(isValidSchedule(schedule), updateSchedule, InvalidScheduleTime)
      } yield schedule

    override def getSchedule(id: ScheduleId): RIO[DBTransactor, Schedule] =
      for {
        transactor <- TransactorService.databaseTransactor
        scheduleOpt <- scheduleRepository.get(id).transact(transactor)
        schedule <- ZIO.fromEither(scheduleOpt.toRight(ScheduleNotFound))
      } yield schedule

    override def getAllSchedules: RIO[DBTransactor, List[Schedule]] =
      for {
        transactor <- TransactorService.databaseTransactor
        schedules <- scheduleRepository.getAll.transact(transactor)
      } yield schedules

    override def getScheduleByTeacherId(teacherId: UserId): RIO[DBTransactor, List[Schedule]] =
      for {
        transactor <- TransactorService.databaseTransactor
        schedules <- scheduleRepository.getByTeacherId(teacherId).transact(transactor)
      } yield schedules

    //TODO
    override def getScheduleByTime(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): RIO[DBTransactor, List[Schedule]] =
      for {
        transactor <- TransactorService.databaseTransactor
        schedules <- scheduleRepository.getByTime(lessonStart, lessonEnd).transact(transactor)
      } yield schedules

    override def isOverlapping(schedule: Schedule): RIO[DBTransactor, Boolean] =
      for {
        _ <- ZIO.cond(isValidSchedule(schedule), (), InvalidScheduleTime)
        transactor <- TransactorService.databaseTransactor
        schedules <- scheduleRepository.findIntersections(schedule).transact(transactor)
        result = schedules match {
          case ::(_, _) => true
          case Nil      => false
        }
      } yield result

    override def deleteSchedule(id: ScheduleId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- scheduleRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[ScheduleRepository, Nothing, ScheduleService] =
    ZLayer.fromService[ScheduleRepository.Service, ScheduleService.Service](repo => new ServiceImpl(repo))
}