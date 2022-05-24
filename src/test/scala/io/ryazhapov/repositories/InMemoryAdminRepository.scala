package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.adminMap
import io.ryazhapov.database.repositories.accounts.AdminRepository
import io.ryazhapov.database.repositories.accounts.AdminRepository.AdminRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Admin
import zio.{Task, ZLayer}

class InMemoryAdminRepository extends AdminRepository.Service {

  override def create(admin: Admin): Task[Unit] = Task.succeed {
    adminMap.put(admin.userId, admin)
    admin.userId
  }

  override def update(admin: Admin): Task[Unit] = Task.succeed {
    adminMap.update(admin.userId, admin)
  }

  override def get(id: UserId): Task[Option[Admin]] = Task.succeed {
    adminMap.get(id)
  }

  override def getAll: Task[List[Admin]] = Task.succeed {
    adminMap.values.toList
  }

  override def delete(id: UserId): Task[Unit] = Task.succeed {
    adminMap.remove(id)
  }
}

object InMemoryAdminRepository {
  def live(): ZLayer[Any, Nothing, AdminRepository] =
    ZLayer.succeed(new InMemoryAdminRepository)
}