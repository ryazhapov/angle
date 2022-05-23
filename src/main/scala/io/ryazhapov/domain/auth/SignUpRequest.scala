package io.ryazhapov.domain.auth

import io.ryazhapov.domain.Password
import io.ryazhapov.domain.accounts.Role

case class SignUpRequest(
  email: String,
  password: Password,
  role: Role
)