package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.{replenishMap, studentMap, userMap}
import io.ryazhapov.database.repositories.billing.ReplenishmentRepository
import io.ryazhapov.database.repositories.billing.ReplenishmentRepository.ReplenishmentRepository
import io.ryazhapov.domain.accounts.Student
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.billing.Replenishment
import io.ryazhapov.domain.{ReplenishmentId, UserId}
import zio.{Task, ZLayer}

class InMemoryReplenishmentRepository extends ReplenishmentRepository.Service {

  override def create(user: User, student: Student, replenishment: Replenishment): Task[ReplenishmentId] = Task.succeed {
    replenishMap.put(replenishment.id, replenishment)
    studentMap.update(student.userId, student)
    userMap.update(user.id, user)
    replenishment.id
  }

  override def getAll: Task[List[Replenishment]] = Task.succeed {
    replenishMap.values.toList
  }

  override def getByStudent(studentId: UserId): Task[List[Replenishment]] = Task.succeed {
    replenishMap.values.filter(_.studentId == studentId).toList
  }
}

object InMemoryReplenishmentRepository {
  def live(): ZLayer[Any, Nothing, ReplenishmentRepository] =
    ZLayer.succeed(new InMemoryReplenishmentRepository)
}
