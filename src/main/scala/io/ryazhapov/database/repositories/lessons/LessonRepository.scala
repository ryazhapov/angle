package io.ryazhapov.database.repositories.lessons

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.LessonId
import io.ryazhapov.domain.accounts.Role.TeacherRole
import io.ryazhapov.domain.accounts.Student
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.lessons.Lesson
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object LessonRepository extends Repository {

  import dbContext._

  type LessonRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, LessonRepository] =
    ZLayer.fromService(new PostgresLessonRepository(_))

  trait Service {
    def create(student: Student, lesson: Lesson): Task[LessonId]

    def update(lesson: Lesson): Task[Unit]

    def get(id: LessonId): Task[Option[Lesson]]

    def getAll: Task[List[Lesson]]

    def getFiltered(completed: Boolean): Task[List[Lesson]]

    def getByUser(user: User): Task[List[Lesson]]

    def getByUserFiltered(user: User, completed: Boolean): Task[List[Lesson]]

    def findUnion(lesson: Lesson): Task[List[Lesson]]

    def delete(student: Student, id: LessonId): Task[Unit]
  }

  class PostgresLessonRepository(xa: Transactor[Task]) extends Service {
    lazy val lessonTable = quote(querySchema[Lesson](""""Lesson""""))
    lazy val studentTable = quote(querySchema[Student](""""Student""""))

    override def create(student: Student, lesson: Lesson): Task[LessonId] = {

      val updateStudent = dbContext.run {
        studentTable
          .filter(_.userId == lift(student.userId))
          .update(lift(student))
      }.unit

      val createLesson = dbContext.run {
        lessonTable
          .insert(lift(lesson))
          .returningGenerated(_.id)
      }

      val transaction = for {
        _ <- updateStudent
        lessonId <- createLesson
      } yield lessonId

      transaction.transact(xa)
    }

    override def update(lesson: Lesson): Task[Unit] =
      dbContext.run {
        lessonTable
          .filter(_.id == lift(lesson.id))
          .update(lift(lesson))
      }.unit.transact(xa)

    override def get(id: LessonId): Task[Option[Lesson]] =
      dbContext.run {
        lessonTable
          .filter(_.id == lift(id))
      }.map(_.headOption).transact(xa)

    override def getAll: Task[List[Lesson]] =
      dbContext.run(lessonTable).transact(xa)

    override def getFiltered(completed: Boolean): Task[List[Lesson]] =
      dbContext.run {
        lessonTable
          .filter(_.completed == lift(completed))
      }.transact(xa)

    override def getByUser(user: User): Task[List[Lesson]] = {
      user.role match {
        case TeacherRole =>
          dbContext.run {
            lessonTable
              .filter(_.teacherId == lift(user.id))
          }.transact(xa)

        case _ =>
          dbContext.run {
            lessonTable
              .filter(_.studentId == lift(user.id))
          }.transact(xa)
      }
    }

    override def getByUserFiltered(user: User, completed: Boolean): Task[List[Lesson]] =
      user.role match {
        case TeacherRole =>
          dbContext.run {
            lessonTable
              .filter(_.teacherId == lift(user.id))
              .filter(_.completed == lift(completed))
          }.transact(xa)

        case _ =>
          dbContext.run {
            lessonTable
              .filter(_.studentId == lift(user.id))
              .filter(_.completed == lift(completed))
          }.transact(xa)
      }

    override def findUnion(lesson: Lesson): Task[List[Lesson]] =
      dbContext.run {
        lessonTable
          .filter(x => x.teacherId == lift(lesson.teacherId) || x.studentId == lift(lesson.studentId))
          .filter(x =>
            !(x.startsAt > lift(lesson.startsAt) && x.startsAt >= lift(lesson.endsAt)) &&
              !(x.endsAt <= lift(lesson.startsAt) && x.endsAt < lift(lesson.endsAt))
          )
      }.transact(xa)

    override def delete(student: Student, id: LessonId): Task[Unit] = {

      val updateStudent = dbContext.run {
        studentTable
          .filter(_.userId == lift(student.userId))
          .update(lift(student))
      }.unit

      val deleteLesson = dbContext.run {
        lessonTable
          .filter(_.id == lift(id))
          .delete
      }.unit

      val transaction = for {
        _ <- updateStudent
        _ <- deleteLesson
      } yield ()

      transaction.transact(xa)
    }
  }
}