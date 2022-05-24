package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.studentMap
import io.ryazhapov.database.repositories.accounts.StudentRepository
import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Student
import zio.{Task, ZLayer}

class InMemoryStudentRepository extends StudentRepository.Service() {

  override def create(student: Student): Task[Unit] = Task.succeed {
    studentMap.put(student.userId, student)
    student.userId
  }

  override def update(student: Student): Task[Unit] = Task.succeed {
    studentMap.update(student.userId, student)
  }

  override def get(id: UserId): Task[Option[Student]] = Task.succeed {
    studentMap.get(id)
  }

  override def getAll: Task[List[Student]] = Task.succeed {
    studentMap.values.toList
  }

  override def delete(id: UserId): Task[Unit] = Task.succeed {
    studentMap.remove(id)
  }
}

object InMemoryStudentRepository {
  def live(): ZLayer[Any, Nothing, StudentRepository] =
    ZLayer.succeed(new InMemoryStudentRepository)
}