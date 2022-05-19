package io.ryazhapov.domain.billing

import io.ryazhapov.domain.{ReplenishmentId, UserId}

case class Replenishment(
  id: ReplenishmentId,
  studentId: UserId,
  amount: Int
)
