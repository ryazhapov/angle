package io.ryazhapov.database.dao.accounts

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.{Level, Student}

case class StudentDao(
  userId: UserId,
  level: Level,
  balance: Int
) {
  def toStudent: Student = Student(userId, level, balance)
}
