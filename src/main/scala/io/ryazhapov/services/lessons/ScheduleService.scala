package io.ryazhapov.services.lessons

import io.ryazhapov.database.repositories.lessons.ScheduleRepository
import io.ryazhapov.domain.lessons.Schedule
import io.ryazhapov.domain.{ScheduleId, UserId}
import io.ryazhapov.errors.{InvalidScheduleTime, ScheduleNotFound}
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
    def createSchedule(schedule: Schedule): Task[Unit]

    def updateSchedule(schedule: Schedule): Task[Unit]

    def getSchedule(id: ScheduleId): Task[Schedule]

    def getAllSchedules: Task[List[Schedule]]

    def getScheduleByTeacherId(teacherId: UserId): Task[List[Schedule]]

    def findScheduleForLesson(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Task[List[Schedule]]

    def isOverlapping(schedule: Schedule): Task[Boolean]

    def deleteSchedule(id: ScheduleId): Task[Unit]
  }

  class ServiceImpl(
    scheduleRepository: ScheduleRepository.Service
  ) extends Service {

    override def createSchedule(schedule: Schedule): Task[Unit] = {
      for {
        _ <- ZIO.cond(schedule.valid, (), InvalidScheduleTime)
        _ <- scheduleRepository.create(schedule)
      } yield ()
    }

    override def updateSchedule(schedule: Schedule): Task[Unit] =
      for {
        _ <- ZIO.cond(schedule.valid, (), InvalidScheduleTime)
        _ <- scheduleRepository.update(schedule)
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

    override def isOverlapping(schedule: Schedule): Task[Boolean] =
      for {
        _ <- ZIO.cond(schedule.valid, (), InvalidScheduleTime)
        schedules <- scheduleRepository.findUnion(schedule)
        result = schedules match {
          case ::(_, _) => true
          case Nil      => false
        }
      } yield result

    override def deleteSchedule(id: ScheduleId): Task[Unit] =
      scheduleRepository.delete(id)
  }
}