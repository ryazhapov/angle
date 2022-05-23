package io.ryazhapov.domain.auth

import io.ryazhapov.domain.Password

case class SignInRequest(
  email: String,
  password: Password
)