package io.ryazhapov.domain.auth

import io.ryazhapov.domain.{SessionId, UserId}

case class Session(
  id: SessionId,
  userId: UserId
)
