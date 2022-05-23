package io.ryazhapov.database.repositories.lessons

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.lessons.Schedule
import io.ryazhapov.domain.{ScheduleId, UserId}
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

import java.time.ZonedDateTime

object ScheduleRepository extends Repository {

  import dbContext._

  type ScheduleRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, ScheduleRepository] =
    ZLayer.fromService(new PostgresScheduleRepository(_))

  trait Service {
    def create(schedule: Schedule): Task[ScheduleId]

    def update(schedule: Schedule): Task[Unit]

    def get(id: ScheduleId): Task[Option[Schedule]]

    def getAll: Task[List[Schedule]]

    def getByTeacher(teacherId: UserId): Task[List[Schedule]]

    def findIntersection(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Task[List[Schedule]]

    def findUnion(id: UserId, start: ZonedDateTime, end: ZonedDateTime): Task[List[Schedule]]

    def delete(id: ScheduleId): Task[Unit]
  }

  class PostgresScheduleRepository(xa: Transactor[Task]) extends Service {

    lazy val scheduleTable = quote(querySchema[Schedule](""""Schedule""""))

    override def create(schedule: Schedule): Task[ScheduleId] =
      dbContext.run {
        scheduleTable
          .insert(lift(schedule))
          .returningGenerated(_.id)
      }.transact(xa)

    override def update(schedule: Schedule): Task[Unit] =
      dbContext.run {
        scheduleTable
          .filter(_.id == lift(schedule.id))
          .update(lift(schedule))
      }.unit.transact(xa)

    override def get(id: ScheduleId): Task[Option[Schedule]] =
      dbContext.run {
        scheduleTable
          .filter(_.id == lift(id))
      }.map(_.headOption).transact(xa)

    override def getAll: Task[List[Schedule]] =
      dbContext.run(scheduleTable).transact(xa)

    override def getByTeacher(teacherId: UserId): Task[List[Schedule]] =
      dbContext.run {
        scheduleTable
          .filter(_.teacherId == lift(teacherId))
      }.transact(xa)

    override def findIntersection(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Task[List[Schedule]] =
      dbContext.run {
        scheduleTable
          .filter(x => x.startsAt <= lift(lessonStart) && x.endsAt >= lift(lessonEnd))
      }.transact(xa)

    override def findUnion(id: UserId, start: ZonedDateTime, end: ZonedDateTime): Task[List[Schedule]] =
      dbContext.run {
        scheduleTable
          .filter(_.teacherId == lift(id))
          .filter(x =>
            !(x.startsAt > lift(start) && x.startsAt >= lift(end)) &&
              !(x.endsAt <= lift(start) && x.endsAt < lift(end))
          )
      }.transact(xa)

    override def delete(id: ScheduleId): Task[Unit] =
      dbContext.run {
        scheduleTable
          .filter(_.id == lift(id))
          .delete
      }.unit.transact(xa)
  }
}
