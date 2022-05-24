package io.ryazhapov.services.lessons

import io.ryazhapov.database.repositories.lessons.ScheduleRepository
import io.ryazhapov.domain.lessons.{Schedule, ScheduleRequest}
import io.ryazhapov.domain.{ScheduleId, UserId}
import io.ryazhapov.errors.{InvalidScheduleTime, ScheduleNotFound, ScheduleOverlapping, UnauthorizedAction}
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

import java.time.ZonedDateTime

@accessible
object ScheduleService {

  type ScheduleService = Has[Service]

  lazy val live = ZLayer.fromService[
    ScheduleRepository.Service,
    ScheduleService.Service
  ](repo => new ServiceImpl(repo))

  trait Service {
    def createSchedule(id: UserId, request: ScheduleRequest): Task[ScheduleId]

    def updateSchedule(id: UserId, scheduleId: ScheduleId, request: ScheduleRequest): Task[Unit]

    def getSchedule(id: ScheduleId): Task[Schedule]

    def getAllSchedules: Task[List[Schedule]]

    def getScheduleByTeacherId(teacherId: UserId): Task[List[Schedule]]

    def findScheduleForLesson(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Task[List[Schedule]]

    def deleteSchedule(id: UserId, scheduleId: ScheduleId): Task[Unit]
  }

  class ServiceImpl(
    scheduleRepository: ScheduleRepository.Service
  ) extends Service {

    override def createSchedule(id: UserId, request: ScheduleRequest): Task[ScheduleId] = {
      for {
        _ <- ZIO.when(!request.isValid)(ZIO.fail(InvalidScheduleTime))
        schedules <- scheduleRepository.findUnion(id, request.startsAt, request.endsAt)
        _ <- schedules match {
          case ::(_, _) => ZIO.fail(ScheduleOverlapping)
          case Nil      => ZIO.succeed(())
        }
        scheduleId <- scheduleRepository.create(Schedule(0, id, request.startsAt, request.endsAt))
      } yield scheduleId
    }

    override def updateSchedule(id: UserId, scheduleId: ScheduleId, request: ScheduleRequest): Task[Unit] =
      for {
        _ <- ZIO.when(!request.isValid)(ZIO.fail(InvalidScheduleTime))
        scheduleOpt <- scheduleRepository.get(scheduleId)
        schedule <- ZIO.fromEither(scheduleOpt.toRight(ScheduleNotFound))
        _ <- ZIO.when(id != schedule.teacherId)(ZIO.fail(UnauthorizedAction))
        schedules <- scheduleRepository.findUnion(id, request.startsAt, request.endsAt)
        _ <- schedules match {
          case ::(_, _) => ZIO.fail(ScheduleOverlapping)
          case Nil      => ZIO.succeed(())
        }
        updated = Schedule(
          schedule.id,
          schedule.teacherId,
          request.startsAt,
          request.endsAt
        )
        _ <- scheduleRepository.update(updated)
      } yield ()

    override def getSchedule(id: ScheduleId): Task[Schedule] =
      for {
        scheduleOpt <- scheduleRepository.get(id)
        schedule <- ZIO.fromEither(scheduleOpt.toRight(ScheduleNotFound))
      } yield schedule

    override def getAllSchedules: Task[List[Schedule]] =
      scheduleRepository.getAll

    override def getScheduleByTeacherId(teacherId: UserId): Task[List[Schedule]] =
      scheduleRepository.getByTeacher(teacherId)

    override def findScheduleForLesson(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Task[List[Schedule]] =
      scheduleRepository.findIntersection(lessonStart, lessonEnd)

    override def deleteSchedule(id: UserId, scheduleId: ScheduleId): Task[Unit] = {
      for {
        scheduleOpt <- scheduleRepository.get(scheduleId)
        schedule <- ZIO.fromEither(scheduleOpt.toRight(ScheduleNotFound))
        _ <- ZIO.when(id != schedule.teacherId)(ZIO.fail(UnauthorizedAction))
        _ <- scheduleRepository.delete(scheduleId)
      } yield ()
    }
  }
}