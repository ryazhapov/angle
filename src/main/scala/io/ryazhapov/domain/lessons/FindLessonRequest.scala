package io.ryazhapov.domain.lessons

import java.time.ZonedDateTime

case class FindLessonRequest(
  startsAt: ZonedDateTime
)
