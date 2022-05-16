package io.ryazhapov.database.dao.accounts

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Admin

case class AdminDao(
  userId: UserId,
) {
  def toAdmin: Admin = Admin(userId)
}
