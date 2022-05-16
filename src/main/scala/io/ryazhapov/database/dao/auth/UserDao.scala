package io.ryazhapov.database.dao.auth

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Role
import io.ryazhapov.domain.auth.User

case class UserDao(
  id: UserId,
  email: String,
  passwordHash: String,
  salt: Array[Byte],
  role: Role,
  isVerified: Boolean
) {
  def toUser: User = User(id, email, role, isVerified)
}
