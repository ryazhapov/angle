package io.ryazhapov.InMemoryRepositories

import io.ryazhapov.database.repositories.auth.SessionRepository
import io.ryazhapov.database.repositories.auth.SessionRepository.SessionRepository
import io.ryazhapov.domain.auth.Session
import io.ryazhapov.domain.{SessionId, UserId}
import zio.{Task, ZLayer}

import scala.collection.concurrent.TrieMap

class InMemorySessionRepository extends SessionRepository.Service {

  private val map = new TrieMap[Int, Int]()

  override def insert(session: Session): Task[SessionId] = Task.succeed {
    map.put(session.id, session.userId)
    session.id
  }

  override def get(id: SessionId): Task[Option[Session]] = Task.succeed {
    val us = map.get(id)
    us match {
      case Some(value) => Some(Session(id, value))
      case None        => None
    }
  }

  override def getByUser(userId: UserId): Task[List[Session]] = Task.succeed {
    map.filter(_._2 == userId).map(a => Session(a._1, a._2)).toList
  }

  override def delete(id: SessionId): Task[Unit] = Task.succeed {
    map.remove(id)
  }
}

object InMemorySessionRepository {
  def live(): ZLayer[Any, Nothing, SessionRepository] =
    ZLayer.succeed(new InMemorySessionRepository)
}