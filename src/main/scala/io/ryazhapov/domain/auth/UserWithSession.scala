package io.ryazhapov.domain.auth

case class UserWithSession(
  user: User,
  session: Session
)
