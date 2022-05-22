package io.ryazhapov.domain.lessons

import io.ryazhapov.domain.{ScheduleId, UserId}

import java.time.ZonedDateTime

case class Schedule(
  id: ScheduleId,
  teacherId: UserId,
  startsAt: ZonedDateTime,
  endsAt: ZonedDateTime
) {
  def valid: Boolean =
    startsAt.isBefore(endsAt) && !startsAt.isEqual(endsAt)
}