package io.ryazhapov.database.repositories.billing

import doobie.implicits._
import doobie.util.transactor.Transactor
import io.ryazhapov.database.repositories.Repository
import io.ryazhapov.database.services.TransactorService.DBTransactor
import io.ryazhapov.domain.accounts.{Student, Teacher}
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.domain.lessons.Lesson
import io.ryazhapov.domain.{LessonId, PaymentId, UserId}
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object PaymentRepository extends Repository {

  import dbContext._

  type PaymentRepository = Has[Service]

  lazy val live: URLayer[DBTransactor, PaymentRepository]
  = ZLayer.fromService(new PostgresPaymentRepository(_))

  trait Service {
    def create(student: Student, teacher: Teacher, lesson: Lesson, payment: Payment): Task[PaymentId]

    def get(id: PaymentId): Task[Option[Payment]]

    def getAll: Task[List[Payment]]

    def getByStudent(studentId: UserId): Task[List[Payment]]

    def getByTeacher(teacherId: UserId): Task[List[Payment]]

    def getByLesson(lessonId: LessonId): Task[Option[Payment]]
  }

  class PostgresPaymentRepository(xa: Transactor[Task]) extends Service {

    lazy val studentTable = quote(querySchema[Student](""""Student""""))
    lazy val teacherTable = quote(querySchema[Teacher](""""Teacher""""))
    lazy val lessonTable = quote(querySchema[Lesson](""""Lesson""""))
    lazy val paymentTable = quote(querySchema[Payment](""""Payment""""))


    override def create(student: Student, teacher: Teacher, lesson: Lesson, payment: Payment): Task[PaymentId] = {

      val updateStudent = dbContext.run {
        studentTable
          .filter(_.userId == lift(student.userId))
          .update(lift(student))
      }.unit

      val updateTeacher = dbContext.run {
        teacherTable
          .filter(_.userId == lift(teacher.userId))
          .update(lift(teacher))
      }.unit

      val updateLesson = dbContext.run {
        lessonTable
          .filter(_.id == lift(lesson.id))
          .update(lift(lesson))
      }.unit

      val createPayment = dbContext.run {
        paymentTable
          .insert(lift(payment))
          .returningGenerated(_.id)
      }

      val transaction = for {
        _ <- updateStudent
        _ <- updateTeacher
        _ <- updateLesson
        paymentId <- createPayment
      } yield paymentId

      transaction.transact(xa)
    }

    override def get(id: PaymentId): Task[Option[Payment]] =
      dbContext.run {
        paymentTable
          .filter(_.id == lift(id))
      }.map(_.headOption).transact(xa)

    override def getAll: Task[List[Payment]] =
      dbContext.run(paymentTable).transact(xa)

    override def getByStudent(studentId: UserId): Task[List[Payment]] =
      dbContext.run {
        paymentTable
          .filter(_.studentId == lift(studentId))
      }.transact(xa)

    override def getByTeacher(teacherId: UserId): Task[List[Payment]] =
      dbContext.run {
        paymentTable
          .filter(_.teacherId == lift(teacherId))
      }.transact(xa)

    override def getByLesson(lessonId: LessonId): Task[Option[Payment]] =
      dbContext.run {
        paymentTable
          .filter(_.lessonId == lift(lessonId))
      }.map(_.headOption).transact(xa)
  }
}