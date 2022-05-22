package io.ryazhapov.services.lessons

import io.ryazhapov.database.repositories.lessons.LessonRepository
import io.ryazhapov.domain.LessonId
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.lessons.Lesson
import io.ryazhapov.errors.{InvalidLessonTime, InvalidScheduleTime, LessonNotFound}
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object LessonService {

  type LessonService = Has[Service]

  lazy val live = ZLayer.fromService[
    LessonRepository.Service,
    LessonService.Service
  ](repo => new ServiceImpl(repo))

  trait Service {
    def createLesson(lesson: Lesson): Task[Unit]

    def completeLesson(id: LessonId): Task[Unit]

    def getLesson(id: LessonId): Task[Lesson]

    def getAllLessons: Task[List[Lesson]]

    def getAllLessonWithFilter(completed: Boolean): Task[List[Lesson]]

    def getLessonsByUser(user: User): Task[List[Lesson]]

    def getLessonsByUserWithFilter(user: User, completed: Boolean): Task[List[Lesson]]

    def isOverlapping(lesson: Lesson): Task[Boolean]

    def deleteLesson(id: LessonId): Task[Unit]
  }

  class ServiceImpl(
    lessonRepository: LessonRepository.Service
  ) extends Service {

    override def createLesson(lesson: Lesson): Task[Unit] =
      for {
        _ <- ZIO.cond(lesson.valid, (), InvalidLessonTime)
        _ <- lessonRepository.create(lesson).unit
      } yield ()

    override def completeLesson(id: LessonId): Task[Unit] =
      for {
        lessonOpt <- lessonRepository.get(id)
        lesson <- ZIO.fromEither(lessonOpt.toRight(LessonNotFound))
        completed = lesson.copy(completed = true)
        _ <- lessonRepository.update(completed).unit
      } yield ()

    override def getLesson(id: LessonId): Task[Lesson] =
      for {
        lessonOpt <- lessonRepository.get(id)
        lesson <- ZIO.fromEither(lessonOpt.toRight(LessonNotFound))
      } yield lesson

    override def getAllLessons: Task[List[Lesson]] =
      lessonRepository.getAll

    override def getAllLessonWithFilter(completed: Boolean): Task[List[Lesson]] =
      lessonRepository.getFiltered(completed)

    override def getLessonsByUser(user: User): Task[List[Lesson]] =
      lessonRepository.getByUser(user)

    override def getLessonsByUserWithFilter(user: User, completed: Boolean): Task[List[Lesson]] =
      lessonRepository.getByUserFiltered(user, completed)

    override def isOverlapping(lesson: Lesson): Task[Boolean] =
      for {
        _ <- ZIO.cond(lesson.valid, (), InvalidScheduleTime)
        schedules <- lessonRepository.findUnion(lesson)
        result = schedules match {
          case ::(_, _) => true
          case Nil      => false
        }
      } yield result

    override def deleteLesson(id: LessonId): Task[Unit] =
      lessonRepository.delete(id)
  }
}