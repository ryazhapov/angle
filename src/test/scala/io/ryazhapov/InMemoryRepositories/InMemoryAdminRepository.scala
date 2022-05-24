package io.ryazhapov.InMemoryRepositories

import io.ryazhapov.database.repositories.accounts.AdminRepository
import io.ryazhapov.database.repositories.accounts.AdminRepository.AdminRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Admin
import zio.{Task, ZLayer}

import scala.collection.concurrent.TrieMap

class InMemoryAdminRepository extends AdminRepository.Service() {

  private val map = new TrieMap[Int, Admin]()

  override def create(admin: Admin): Task[Unit] = Task.succeed {
    map.put(admin.userId, admin)
    admin.userId
  }

  override def update(admin: Admin): Task[Unit] = Task.succeed {
    map.update(admin.userId, admin)
  }

  override def get(id: UserId): Task[Option[Admin]] = Task.succeed {
    map.get(id)
  }

  override def getAll: Task[List[Admin]] = Task.succeed {
    map.values.toList
  }

  override def delete(id: UserId): Task[Unit] = Task.succeed {
    map.remove(id)
  }
}

object InMemoryAdminRepository {
  def live(): ZLayer[Any, Nothing, AdminRepository] =
    ZLayer.succeed(new InMemoryAdminRepository)
}