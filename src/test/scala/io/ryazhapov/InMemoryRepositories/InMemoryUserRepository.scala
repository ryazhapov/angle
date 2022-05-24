package io.ryazhapov.InMemoryRepositories

import io.ryazhapov.database.repositories.auth.UserRepository
import io.ryazhapov.database.repositories.auth.UserRepository.UserRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.auth.User
import zio.{Task, ZLayer}

import scala.collection.concurrent.TrieMap

class InMemoryUserRepository extends UserRepository.Service {

  private val map = new TrieMap[Int, User]()

  override def create(user: User): Task[UserId] = Task.succeed {
    map.put(user.id, user)
    user.id
  }

  override def update(user: User): Task[Unit] = Task.succeed {
    map.update(user.id, user)
  }

  override def get(id: UserId): Task[Option[User]] = Task.succeed {
    map.get(id)
  }

  override def verify(id: UserId): Task[Unit] = Task.succeed {
    val user = map.get(id)
    user match {
      case Some(value) => map.update(id, value.copy(verified = true))
      case None        => ()
    }
  }

  override def getAll: Task[List[User]] = Task.succeed {
    map.values.toList
  }

  override def getUnverified: Task[List[User]] = Task.succeed {
    map.values.filter(_.verified == false).toList
  }

  override def getByEmail(email: String): Task[Option[User]] = Task.succeed {
    map.values.find(_.email == email)
  }

  override def delete(id: UserId): Task[Unit] = Task.succeed {
    map.remove(id)
  }

  override def deleteByEmail(email: String): Task[Unit] = Task.succeed {
    val user = map.find(_._2.email == email)
    user match {
      case Some(value) => map.remove(value._1)
      case None        => ()
    }
  }
}

object InMemoryUserRepository {
  def live(): ZLayer[Any, Nothing, UserRepository] =
    ZLayer.succeed(new InMemoryUserRepository)
}
