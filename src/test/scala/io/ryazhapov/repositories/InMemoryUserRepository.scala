package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.userMap
import io.ryazhapov.database.repositories.auth.UserRepository
import io.ryazhapov.database.repositories.auth.UserRepository.UserRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.auth.User
import zio.{Task, ZLayer}

class InMemoryUserRepository extends UserRepository.Service {

  override def create(user: User): Task[UserId] = Task.succeed {
    userMap.put(user.id, user)
    user.id
  }

  override def update(user: User): Task[Unit] = Task.succeed {
    userMap.update(user.id, user)
  }

  override def get(id: UserId): Task[Option[User]] = Task.succeed {
    userMap.get(id)
  }

  override def verify(id: UserId): Task[Unit] = Task.succeed {
    val user = userMap.get(id)
    user match {
      case Some(value) => userMap.update(id, value.copy(verified = true))
      case None        => ()
    }
  }

  override def getAll: Task[List[User]] = Task.succeed {
    userMap.values.toList
  }

  override def getUnverified: Task[List[User]] = Task.succeed {
    userMap.values.filter(_.verified == false).toList
  }

  override def getByEmail(email: String): Task[Option[User]] = Task.succeed {
    userMap.values.find(_.email == email)
  }

  override def delete(id: UserId): Task[Unit] = Task.succeed {
    userMap.remove(id)
  }

  override def deleteByEmail(email: String): Task[Unit] = Task.succeed {
    val user = userMap.find(_._2.email == email)
    user match {
      case Some(value) => userMap.remove(value._1)
      case None        => ()
    }
  }
}

object InMemoryUserRepository {
  def live(): ZLayer[Any, Nothing, UserRepository] =
    ZLayer.succeed(new InMemoryUserRepository)
}