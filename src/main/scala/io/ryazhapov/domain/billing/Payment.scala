package io.ryazhapov.domain.billing

import io.ryazhapov.domain.{LessonId, PaymentId, UserId}

case class Payment(
  id: PaymentId,
  studentId: UserId,
  teacherId: UserId,
  lessonId: LessonId,
  amount: Int
)
