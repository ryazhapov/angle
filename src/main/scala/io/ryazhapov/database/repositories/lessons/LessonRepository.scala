package io.ryazhapov.database.repositories.lessons

import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.domain.LessonId
import io.ryazhapov.domain.accounts.Role.TeacherRole
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.lessons.Lesson
import zio.{Has, ULayer, ZLayer}

object LessonRepository extends Repository {

  import dbContext._

  type LessonRepository = Has[Service]

  trait Service {
    def create(lesson: Lesson): Result[Unit]

    def update(lesson: Lesson): Result[Unit]

    def get(id: LessonId): Result[Option[Lesson]]

    def getAll: Result[List[Lesson]]

    def getFiltered(completed: Boolean): Result[List[Lesson]]

    def getByUser(user: User): Result[List[Lesson]]

    def getByUserFiltered(user: User, completed: Boolean): Result[List[Lesson]]

    def findUnion(lesson: Lesson): Result[List[Lesson]]

    def delete(id: LessonId): Result[Unit]
  }

  class ServiceImpl() extends Service {
    lazy val lessonTable = quote {
      querySchema[Lesson](""""Lesson"""")
    }

    override def create(lesson: Lesson): Result[Unit] =
      dbContext.run(lessonTable
        .insert(lift(lesson))
      ).unit

    override def update(lesson: Lesson): Result[Unit] =
      dbContext.run(lessonTable
        .filter(_.id == lift(lesson.id))
        .update(lift(lesson))
      ).unit

    override def get(id: LessonId): Result[Option[Lesson]] =
      dbContext.run(lessonTable
        .filter(_.id == lift(id))
      ).map(_.headOption)

    override def getAll: Result[List[Lesson]] =
      dbContext.run(lessonTable)

    override def getFiltered(completed: Boolean): Result[List[Lesson]] =
      dbContext.run(lessonTable
        .filter(_.completed == lift(completed))
      )

    override def getByUser(user: User): Result[List[Lesson]] = {
      user.role match {
        case TeacherRole =>
          dbContext.run(lessonTable
            .filter(_.teacherId == lift(user.id))
          )

        case _ =>
          dbContext.run(lessonTable
            .filter(_.studentId == lift(user.id))
          )
      }
    }

    override def getByUserFiltered(user: User, completed: Boolean): Result[List[Lesson]] =
      user.role match {
        case TeacherRole =>
          dbContext.run(lessonTable
            .filter(_.teacherId == lift(user.id))
            .filter(_.completed == lift(completed))
          )

        case _ =>
          dbContext.run(lessonTable
            .filter(_.studentId == lift(user.id))
            .filter(_.completed == lift(completed))
          )
      }

    override def findUnion(lesson: Lesson): Result[List[Lesson]] =
      dbContext.run(lessonTable
        .filter(x => x.teacherId == lift(lesson.teacherId) || x.studentId == lift(lesson.studentId))
        .filter(x =>
          !(x.startsAt > lift(lesson.startsAt) && x.startsAt >= lift(lesson.endsAt)) &&
            !(x.endsAt <= lift(lesson.startsAt) && x.endsAt < lift(lesson.endsAt))
        )
      )

    override def delete(id: LessonId): Result[Unit] =
      dbContext.run(lessonTable
        .filter(_.id == lift(id))
        .delete
      ).unit
  }

  lazy val live: ULayer[LessonRepository] =
    ZLayer.succeed(new ServiceImpl())
}
