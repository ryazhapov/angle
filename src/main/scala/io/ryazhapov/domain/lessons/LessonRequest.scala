package io.ryazhapov.domain.lessons

import io.ryazhapov.domain.UserId

import java.time.ZonedDateTime

case class LessonRequest(
  teacherId: UserId,
  startsAt: ZonedDateTime,
  endsAt: ZonedDateTime
) {
  def isValid: Boolean =
    startsAt.isBefore(endsAt) && !startsAt.isEqual(endsAt)
}