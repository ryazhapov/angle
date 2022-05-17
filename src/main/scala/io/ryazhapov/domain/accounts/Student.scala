package io.ryazhapov.domain.accounts

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Level.Beginner

case class Student(
  userId: UserId,
  level: Level = Beginner,
  balance: Int = 0
)