package io.ryazhapov.domain.accounts

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Level.UpperIntermediate

case class Teacher(
  userId: UserId,
  level: Level = UpperIntermediate,
  native: Boolean = false,
  rate: Int = 500,
  balance: Int = 0
)