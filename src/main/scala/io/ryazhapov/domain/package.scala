package io.ryazhapov

import java.util.UUID

package object domain {
  type UserId = UUID
  type SessionId = UUID
  type Password = String

  type ScheduleId = UUID
  type LessonId = UUID

  type ReplenishmentId = UUID
  type PaymentId = UUID
  type WithdrawalId = UUID
}
