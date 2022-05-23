package io.ryazhapov.domain.lessons

import java.time.ZonedDateTime

case class ScheduleRequest(
  startsAt: ZonedDateTime,
  endsAt: ZonedDateTime
) {
  def isValid: Boolean =
    startsAt.isBefore(endsAt) && !startsAt.isEqual(endsAt)
}