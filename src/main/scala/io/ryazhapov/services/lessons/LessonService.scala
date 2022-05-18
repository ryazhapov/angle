package io.ryazhapov.services.lessons

import io.ryazhapov.database.repositories.lessons.LessonRepository
import io.ryazhapov.database.repositories.lessons.LessonRepository.LessonRepository
import io.ryazhapov.database.services.TransactorService
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.LessonId
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.lessons.Lesson
import io.ryazhapov.errors.{InvalidLessonTime, InvalidScheduleTime, LessonNotFound}
import zio.interop.catz._
import zio.macros.accessible
import zio.{Has, RIO, ZIO, ZLayer}

@accessible
object LessonService {
  type LessonService = Has[Service]

  trait Service {
    def createLesson(lesson: Lesson): RIO[DBTransactor, Unit]

    def updateLesson(lesson: Lesson): RIO[DBTransactor, Unit]

    def getLesson(id: LessonId): RIO[DBTransactor, Lesson]

    def getAllLessons: RIO[DBTransactor, List[Lesson]]

    def getAllLessonWithFilter(completed: Boolean): RIO[DBTransactor, List[Lesson]]

    def getLessonsByUser(user: User): RIO[DBTransactor, List[Lesson]]

    def getLessonsByUserWithFilter(user: User, completed: Boolean): RIO[DBTransactor, List[Lesson]]

    def isOverlapping(lesson: Lesson): RIO[DBTransactor, Boolean]

    def deleteLesson(id: LessonId): RIO[DBTransactor, Unit]
  }

  def isValidLesson(lesson: Lesson): Boolean =
    lesson.startsAt.isBefore(lesson.endsAt) && !lesson.startsAt.isEqual(lesson.endsAt)

  class ServiceImpl(
    lessonRepository: LessonRepository.Service
  ) extends Service {

    import doobie.implicits._

    override def createLesson(lesson: Lesson): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- ZIO.cond(isValidLesson(lesson), (), InvalidLessonTime)
        _ <- lessonRepository.create(lesson).transact(transactor).unit
      } yield ()

    override def updateLesson(lesson: Lesson): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        updateLesson = lessonRepository.update(lesson).transact(transactor)
        _ <- ZIO.cond(isValidLesson(lesson), updateLesson, InvalidLessonTime).unit
      } yield ()

    override def getLesson(id: LessonId): RIO[DBTransactor, Lesson] =
      for {
        transactor <- TransactorService.databaseTransactor
        lessonOpt <- lessonRepository.get(id).transact(transactor)
        lesson <- ZIO.fromEither(lessonOpt.toRight(LessonNotFound))
      } yield lesson

    override def getAllLessons: RIO[DBTransactor, List[Lesson]] =
      for {
        transactor <- TransactorService.databaseTransactor
        lessons <- lessonRepository.getAll.transact(transactor)
      } yield lessons

    override def getAllLessonWithFilter(completed: Boolean): RIO[DBTransactor, List[Lesson]] =
      for {
        transactor <- TransactorService.databaseTransactor
        lessons <- lessonRepository.getFiltered(completed).transact(transactor)
      } yield lessons

    override def getLessonsByUser(user: User): RIO[DBTransactor, List[Lesson]] =
      for {
        transactor <- TransactorService.databaseTransactor
        lessons <- lessonRepository.getByUser(user).transact(transactor)
      } yield lessons

    override def getLessonsByUserWithFilter(user: User, completed: Boolean): RIO[DBTransactor, List[Lesson]] =
      for {
        transactor <- TransactorService.databaseTransactor
        lessons <- lessonRepository.getByUserFiltered(user, completed).transact(transactor)
      } yield lessons

    override def isOverlapping(lesson: Lesson): RIO[DBTransactor, Boolean] =
      for {
        _ <- ZIO.cond(isValidLesson(lesson), (), InvalidScheduleTime)
        transactor <- TransactorService.databaseTransactor
        schedules <- lessonRepository.findUnion(lesson).transact(transactor)
        result = schedules match {
          case ::(_, _) => true
          case Nil      => false
        }
      } yield result

    override def deleteLesson(id: LessonId): RIO[DBTransactor, Unit] =
      for {
        transactor <- TransactorService.databaseTransactor
        _ <- lessonRepository.delete(id).transact(transactor)
      } yield ()
  }

  lazy val live: ZLayer[LessonRepository, Nothing, LessonService] =
    ZLayer.fromService[LessonRepository.Service, LessonService.Service](repo => new ServiceImpl(repo))
}