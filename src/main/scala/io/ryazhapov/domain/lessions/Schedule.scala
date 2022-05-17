package io.ryazhapov.domain.lessions

import io.ryazhapov.domain.{ScheduleId, UserId}

import java.time.ZonedDateTime

case class Schedule(
  id: ScheduleId,
  teacherId: UserId,
  startsAt: ZonedDateTime,
  endsAt: ZonedDateTime
)