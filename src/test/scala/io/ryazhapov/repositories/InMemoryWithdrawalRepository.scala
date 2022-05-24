package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.{teacherMap, withdrawalMap}
import io.ryazhapov.database.repositories.billing.WithdrawalRepository
import io.ryazhapov.database.repositories.billing.WithdrawalRepository.WithdrawalRepository
import io.ryazhapov.domain.accounts.Teacher
import io.ryazhapov.domain.billing.Withdrawal
import io.ryazhapov.domain.{UserId, WithdrawalId}
import zio.{Task, ZLayer}

class InMemoryWithdrawalRepository extends WithdrawalRepository.Service {

  override def create(teacher: Teacher, withdrawal: Withdrawal): Task[WithdrawalId] =
    Task.succeed {
      teacherMap.update(teacher.userId, teacher)
      withdrawalMap.put(withdrawal.id, withdrawal)
      withdrawal.id
    }

  override def getAll: Task[List[Withdrawal]] = Task.succeed {
    withdrawalMap.values.toList
  }

  override def getByTeacher(teacherId: UserId): Task[List[Withdrawal]] = Task.succeed {
    withdrawalMap.values.filter(_.teacherId == teacherId).toList
  }
}

object InMemoryWithdrawalRepository {
  def live(): ZLayer[Any, Nothing, WithdrawalRepository] =
    ZLayer.succeed(new InMemoryWithdrawalRepository)
}