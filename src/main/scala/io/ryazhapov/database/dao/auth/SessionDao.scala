package io.ryazhapov.database.dao.auth

import io.ryazhapov.domain.auth.Session
import io.ryazhapov.domain.{SessionId, UserId, auth}

case class SessionDao(
  id: SessionId,
  userId: UserId
) {
  def toSession: Session = auth.Session(id, userId)
}
