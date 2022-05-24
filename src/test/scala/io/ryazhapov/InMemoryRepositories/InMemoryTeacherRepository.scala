package io.ryazhapov.InMemoryRepositories

import io.ryazhapov.database.repositories.accounts.TeacherRepository
import io.ryazhapov.database.repositories.accounts.TeacherRepository.TeacherRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Teacher
import zio.{Task, ZLayer}

import scala.collection.concurrent.TrieMap

class InMemoryTeacherRepository extends TeacherRepository.Service() {

  private val map = new TrieMap[Int, Teacher]()

  override def create(teacher: Teacher): Task[Unit] = Task.succeed {
    map.put(teacher.userId, teacher)
    teacher.userId
  }

  override def update(teacher: Teacher): Task[Unit] = Task.succeed {
    map.update(teacher.userId, teacher)
  }

  override def get(id: UserId): Task[Option[Teacher]] = Task.succeed {
    map.get(id)
  }

  override def getAll: Task[List[Teacher]] = Task.succeed {
    map.values.toList
  }

  override def delete(id: UserId): Task[Unit] = Task.succeed {
    map.remove(id)
  }
}

object InMemoryTeacherRepository {
  def live(): ZLayer[Any, Nothing, TeacherRepository] =
    ZLayer.succeed(new InMemoryTeacherRepository)
}