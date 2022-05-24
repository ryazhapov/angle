package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.{lessonMap, studentMap}
import io.ryazhapov.database.repositories.lessons.LessonRepository
import io.ryazhapov.database.repositories.lessons.LessonRepository.LessonRepository
import io.ryazhapov.domain.LessonId
import io.ryazhapov.domain.accounts.Role.TeacherRole
import io.ryazhapov.domain.accounts.Student
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.lessons.Lesson
import zio.{Task, ZLayer}

class InMemoryLessonRepository extends LessonRepository.Service {

  import ZonedDateTimeExtension._

  override def create(student: Student, lesson: Lesson): Task[LessonId] = Task.succeed {
    lessonMap.put(lesson.id, lesson)
    studentMap.update(student.userId, student)
    lesson.id
  }

  override def update(lesson: Lesson): Task[Unit] = Task.succeed {
    lessonMap.update(lesson.id, lesson)
  }

  override def get(id: LessonId): Task[Option[Lesson]] = Task.succeed {
    lessonMap.get(id)
  }

  override def getAll: Task[List[Lesson]] = Task.succeed {
    lessonMap.values.toList
  }

  override def getFiltered(completed: Boolean): Task[List[Lesson]] = Task.succeed {
    lessonMap.values.filter(_.completed == completed).toList
  }

  override def getByUser(user: User): Task[List[Lesson]] = Task.succeed {
    user.role match {
      case TeacherRole => lessonMap.values.filter(_.teacherId == user.id).toList
      case _           => lessonMap.values.filter(_.studentId == user.id).toList
    }
  }

  override def getByUserFiltered(user: User, completed: Boolean): Task[List[Lesson]] = Task.succeed {
    user.role match {
      case TeacherRole => lessonMap.values.filter(_.teacherId == user.id).filter(_.completed == completed).toList
      case _           => lessonMap.values.filter(_.studentId == user.id).filter(_.completed == completed).toList
    }
  }

  override def findUnion(lesson: Lesson): Task[List[Lesson]] = Task.succeed {
    lessonMap.values.filter(x => x.teacherId == lesson.teacherId || x.studentId == lesson.studentId)
      .filter(x => !(x.startsAt > lesson.startsAt && x.startsAt >= lesson.endsAt) &&
        !(x.endsAt <= lesson.startsAt) && x.endsAt < lesson.endsAt).toList
  }

  override def delete(student: Student, id: LessonId): Task[Unit] = Task.succeed {
    studentMap.update(student.userId, student)
    lessonMap.remove(id)
  }
}

object InMemoryLessonRepository {
  def live(): ZLayer[Any, Nothing, LessonRepository] =
    ZLayer.succeed(new InMemoryLessonRepository)
}