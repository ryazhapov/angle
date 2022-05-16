package io.ryazhapov.domain.accounts

import io.ryazhapov.database.dao.accounts.AdminDao
import io.ryazhapov.domain.UserId

case class Admin(
  userId: UserId
) {
  def toDao: AdminDao = AdminDao(userId)
}
