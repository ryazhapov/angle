package io.ryazhapov.domain.lessons

import io.ryazhapov.domain.{LessonId, UserId}

import java.time.ZonedDateTime

case class Lesson(
  id: LessonId,
  teacherId: UserId,
  studentId: UserId,
  startsAt: ZonedDateTime,
  endsAt: ZonedDateTime,
  completed: Boolean
)