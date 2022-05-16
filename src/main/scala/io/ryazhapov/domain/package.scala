package io.ryazhapov

import java.util.UUID

package object domain {
  type UserId = UUID
  type SessionId = UUID
  type Password = String

  type AvailableSlotId = UUID
  type BookedSlotId = UUID

  type TransactionId = UUID
  type ReplenishmentId = UUID
  type WithdrawalId = UUID
}
