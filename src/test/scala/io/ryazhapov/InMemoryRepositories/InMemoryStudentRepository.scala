package io.ryazhapov.InMemoryRepositories

import io.ryazhapov.database.repositories.accounts.StudentRepository
import io.ryazhapov.database.repositories.accounts.StudentRepository.StudentRepository
import io.ryazhapov.domain.UserId
import io.ryazhapov.domain.accounts.Student
import zio.{Task, ZLayer}

import scala.collection.concurrent.TrieMap

class InMemoryStudentRepository extends StudentRepository.Service() {

  private val map = new TrieMap[Int, Student]()

  override def create(student: Student): Task[Unit] = Task.succeed {
    map.put(student.userId, student)
    student.userId
  }

  override def update(student: Student): Task[Unit] = Task.succeed {
    map.update(student.userId, student)
  }

  override def get(id: UserId): Task[Option[Student]] = Task.succeed {
    map.get(id)
  }

  override def getAll: Task[List[Student]] = Task.succeed {
    map.values.toList
  }

  override def delete(id: UserId): Task[Unit] = Task.succeed {
    map.remove(id)
  }
}

object InMemoryStudentRepository {
  def live(): ZLayer[Any, Nothing, StudentRepository] =
    ZLayer.succeed(new InMemoryStudentRepository)
}