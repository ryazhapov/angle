package io.ryazhapov.database.repositories.lessons

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.lessons.Schedule
import io.ryazhapov.domain.{ScheduleId, UserId}
import zio.{Has, ULayer, ZLayer}

import java.time.ZonedDateTime

object ScheduleRepository extends Repository {

  import dbContext._

  type ScheduleRepository = Has[Service]
  lazy val live: ULayer[ScheduleRepository] =
    ZLayer.succeed(new ServiceImpl())

  trait Service {
    def create(schedule: Schedule): Result[Unit]

    def update(schedule: Schedule): Result[Unit]

    def get(id: ScheduleId): Result[Option[Schedule]]

    def getAll: Result[List[Schedule]]

    def getByTeacher(teacherId: UserId): Result[List[Schedule]]

    def findIntersection(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Result[List[Schedule]]

    def findUnion(schedule: Schedule): Result[List[Schedule]]

    def delete(id: ScheduleId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val scheduleTable: Quoted[EntityQuery[Schedule]] = quote {
      querySchema[Schedule](""""Schedule"""")
    }

    override def create(schedule: Schedule): Result[Unit] =
      dbContext.run(scheduleTable
        .insert(lift(schedule))
      ).unit

    override def update(schedule: Schedule): Result[Unit] =
      dbContext.run(scheduleTable
        .filter(_.id == lift(schedule.id))
        .update(lift(schedule))
      ).unit

    override def get(id: ScheduleId): Result[Option[Schedule]] =
      dbContext.run(scheduleTable
        .filter(_.id == lift(id))
      ).map(_.headOption)

    override def getAll: Result[List[Schedule]] =
      dbContext.run(scheduleTable)

    override def getByTeacher(teacherId: UserId): Result[List[Schedule]] =
      dbContext.run(scheduleTable
        .filter(_.teacherId == lift(teacherId))
      )

    override def findIntersection(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Result[List[Schedule]] =
      dbContext.run(scheduleTable
        .filter(x => x.startsAt <= lift(lessonStart) && x.endsAt >= lift(lessonEnd))
      )

    override def findUnion(schedule: Schedule): Result[List[Schedule]] =
      dbContext.run(scheduleTable
        .filter(_.teacherId == lift(schedule.teacherId))
        .filter(x =>
          !(x.startsAt > lift(schedule.startsAt) && x.startsAt >= lift(schedule.endsAt)) &&
            !(x.endsAt <= lift(schedule.startsAt) && x.endsAt < lift(schedule.endsAt))
        )
      )

    override def delete(id: ScheduleId): Result[Unit] =
      dbContext.run(scheduleTable
        .filter(_.id == lift(id))
        .delete
      ).unit
  }
}
