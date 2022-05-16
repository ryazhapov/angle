package io.ryazhapov.database.dao.accounts

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.{Level, Teacher}

case class TeacherDao(
  userId: UserId,
  level: Level,
  isNative: Boolean,
  balance: Int
) {
  def toTeacher: Teacher = Teacher(userId, level, isNative, balance)
}
