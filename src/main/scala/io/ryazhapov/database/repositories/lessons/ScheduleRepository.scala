package io.ryazhapov.database.repositories.lessons

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.lessions.Schedule
import io.ryazhapov.domain.{ScheduleId, UserId}
import zio.{Has, ULayer, ZLayer}

import java.sql.{Timestamp, Types}
import java.time.{ZoneId, ZonedDateTime}

object ScheduleRepository extends Repository {

  import dbContext._

  type ScheduleRepository = Has[Service]

  implicit val zoneDateTimeEncoder: Encoder[ZonedDateTime] =
    encoder(Types.TIMESTAMP_WITH_TIMEZONE, (index, value, row) =>
      row.setTimestamp(index, Timestamp.from(value.toInstant)))

  implicit val zoneDateTimeDecoder: Decoder[ZonedDateTime] = decoder((index, row) => {
    ZonedDateTime.ofInstant(row.getTimestamp(index).toInstant, ZoneId.of("UTC"))
  })

  implicit class ZonedDateTimeQuotes(left: ZonedDateTime) {
    def >(right: ZonedDateTime): Quoted[Boolean] = quote(infix"$left > $right".as[Boolean])

    def <(right: ZonedDateTime): Quoted[Boolean] = quote(infix"$left < $right".as[Boolean])

    def >=(right: ZonedDateTime): Quoted[Boolean] = quote(infix"$left >= $right".as[Boolean])

    def <=(right: ZonedDateTime): Quoted[Boolean] = quote(infix"$left <= $right".as[Boolean])
  }

  trait Service {
    def create(schedule: Schedule): Result[Unit]

    def update(schedule: Schedule): Result[Unit]

    def get(id: ScheduleId): Result[Option[Schedule]]

    def getAll: Result[List[Schedule]]

    def getByTeacherId(teacherId: UserId): Result[List[Schedule]]

    def getByTime(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Result[List[Schedule]]

    def findIntersections(schedule: Schedule): Result[List[Schedule]]

    def delete(id: ScheduleId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val scheduleTable: Quoted[EntityQuery[Schedule]] = quote {
      querySchema[Schedule](""""Schedule"""")
    }

    override def create(schedule: Schedule): Result[Unit] =
      dbContext.run(scheduleTable.insert(lift(schedule))).unit

    override def update(schedule: Schedule): Result[Unit] =
      dbContext.run(scheduleTable.filter(_.id == lift(schedule.id)).update(lift(schedule))).unit

    override def get(id: ScheduleId): Result[Option[Schedule]] =
      dbContext.run(scheduleTable.filter(_.id == lift(id))).map(_.headOption)

    override def getAll: Result[List[Schedule]] =
      dbContext.run(scheduleTable)

    override def getByTeacherId(teacherId: UserId): Result[List[Schedule]] =
      dbContext.run(scheduleTable.filter(_.teacherId == lift(teacherId)))

    override def getByTime(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Result[List[Schedule]] =
      dbContext.run(scheduleTable.filter(x => x.startsAt <= lift(lessonStart) && x.endsAt >= lift(lessonEnd)))

    override def findIntersections(schedule: Schedule): Result[List[Schedule]] =
      dbContext.run(scheduleTable
        .filter(_.teacherId == lift(schedule.teacherId))
        .filter(x =>
          !(x.startsAt > lift(schedule.startsAt) && x.startsAt >= lift(schedule.endsAt)) &&
            !(x.endsAt <= lift(schedule.startsAt) && x.endsAt < lift(schedule.endsAt))
        ))

    override def delete(id: ScheduleId): Result[Unit] =
      dbContext.run(scheduleTable.filter(_.id == lift(id)).delete).unit
  }

  lazy val live: ULayer[ScheduleRepository] =
    ZLayer.succeed(new ServiceImpl())
}
