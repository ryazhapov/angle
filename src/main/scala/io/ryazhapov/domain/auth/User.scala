package io.ryazhapov.domain.auth

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Role

case class User(
  id: UserId,
  email: String,
  passwordHash: String,
  salt: Array[Byte],
  role: Role,
  verified: Boolean
)