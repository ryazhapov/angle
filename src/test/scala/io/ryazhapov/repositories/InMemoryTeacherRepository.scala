package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.teacherMap
import io.ryazhapov.database.repositories.accounts.TeacherRepository
import io.ryazhapov.database.repositories.accounts.TeacherRepository.TeacherRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Teacher
import zio.{Task, ZLayer}

class InMemoryTeacherRepository extends TeacherRepository.Service() {

  override def create(teacher: Teacher): Task[Unit] = Task.succeed {
    teacherMap.put(teacher.userId, teacher)
    teacher.userId
  }

  override def update(teacher: Teacher): Task[Unit] = Task.succeed {
    teacherMap.update(teacher.userId, teacher)
  }

  override def get(id: UserId): Task[Option[Teacher]] = Task.succeed {
    teacherMap.get(id)
  }

  override def getAll: Task[List[Teacher]] = Task.succeed {
    teacherMap.values.toList
  }

  override def delete(id: UserId): Task[Unit] = Task.succeed {
    teacherMap.remove(id)
  }
}

object InMemoryTeacherRepository {
  def live(): ZLayer[Any, Nothing, TeacherRepository] =
    ZLayer.succeed(new InMemoryTeacherRepository)
}