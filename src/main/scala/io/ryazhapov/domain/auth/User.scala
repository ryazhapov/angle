package io.ryazhapov.domain.auth

import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Role

case class User(
  id: UserId,
  email: String,
  role: Role,
  verified: Boolean,
  passwordHash: String,
  salt: Array[Byte]
)