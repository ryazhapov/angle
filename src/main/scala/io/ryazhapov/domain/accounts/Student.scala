package io.ryazhapov.domain.accounts

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Level.{Beginner, UpperIntermediate}

case class Student(
  userId: UserId,
  level: Level = Beginner,
  target: Level = UpperIntermediate,
  balance: Int = 0,
  reserved: Int = 0
)