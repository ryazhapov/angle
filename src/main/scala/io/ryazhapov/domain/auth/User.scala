package io.ryazhapov.domain.auth

import io.ryazhapov.database.dao.auth.UserDao
import io.ryazhapov.domain.accounts.Role
import io.ryazhapov.domain.accounts.Role.StudentRole
import io.ryazhapov.domain.{Password, UserId}
import io.ryazhapov.utils.SecurityUtils

case class User(
  id: UserId,
  email: String,
  role: Role = StudentRole,
  isVerified: Boolean
) {
  def toDAO(password: Password): UserDao = {
    val salt = SecurityUtils.generateSalt()
    UserDao(id, email, SecurityUtils.countSecretHash(password, salt), salt, role, isVerified)
  }
}
