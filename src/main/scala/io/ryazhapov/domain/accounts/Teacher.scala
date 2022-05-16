package io.ryazhapov.domain.accounts

import io.ryazhapov.database.dao.accounts.TeacherDao
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Level.UpperIntermediate

case class Teacher(
  userId: UserId,
  level: Level = UpperIntermediate,
  isNative: Boolean = false,
  balance: Int = 0
) {
  def toDao: TeacherDao = TeacherDao(userId, level, isNative, balance)
}
