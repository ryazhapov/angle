package io.ryazhapov.services.lessons

import io.ryazhapov.database.repositories.accounts.{StudentRepository, TeacherRepository}
import io.ryazhapov.database.repositories.lessons.{LessonRepository, ScheduleRepository}
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.lessons.{Lesson, LessonRequest}
import io.ryazhapov.domain.{LessonId, UserId}
import io.ryazhapov.errors._
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object LessonService {

  type LessonService = Has[Service]

  lazy val live = ZLayer.fromServices[
    StudentRepository.Service,
    TeacherRepository.Service,
    ScheduleRepository.Service,
    LessonRepository.Service,
    LessonService.Service
  ]((studentRepo, teacherRepo, scheduleRepo, lessonRepo) =>
    new ServiceImpl(studentRepo, teacherRepo, scheduleRepo, lessonRepo))

  trait Service {
    def createLesson(id: UserId, request: LessonRequest): Task[LessonId]

    def getLesson(id: LessonId): Task[Lesson]

    def getAllLessons: Task[List[Lesson]]

    def getAllLessonWithFilter(completed: Boolean): Task[List[Lesson]]

    def getLessonsByUser(user: User): Task[List[Lesson]]

    def getLessonsByUserWithFilter(user: User, completed: Boolean): Task[List[Lesson]]

    def deleteLesson(id: UserId, lessonId: LessonId): Task[Unit]
  }

  class ServiceImpl(
    studentRepository: StudentRepository.Service,
    teacherRepository: TeacherRepository.Service,
    scheduleRepository: ScheduleRepository.Service,
    lessonRepository: LessonRepository.Service
  ) extends Service {

    override def createLesson(id: UserId, request: LessonRequest): Task[LessonId] =
      for {
        studentOpt <- studentRepository.get(id)
        student <- ZIO.fromEither(studentOpt.toRight(StudentNotFound))
        teacherOpt <- teacherRepository.get(request.teacherId)
        teacher <- ZIO.fromEither(teacherOpt.toRight(TeacherNotFound))
        lessonEnd = request.startsAt.plusHours(1)
        _ <- ZIO.when(student.balance < teacher.rate)(ZIO.fail(NotEnoughMoney))
        _ <- scheduleRepository.findIntersection(request.startsAt, lessonEnd).flatMap {
          case ::(_, _) => ZIO.succeed(())
          case Nil      => ZIO.fail(ScheduleNotFound)
        }
        lesson = Lesson(
          0,
          teacher.userId,
          id,
          request.startsAt,
          lessonEnd,
          completed = false
        )
        schedules <- lessonRepository.findUnion(lesson)
        _ <- schedules match {
          case ::(_, _) => ZIO.fail(LessonOverlapping)
          case Nil      => ZIO.succeed(())
        }
        updStudent = student.copy(
          balance = student.balance - teacher.rate,
          reserved = student.reserved + teacher.rate
        )
        lessonId <- lessonRepository.create(updStudent, lesson)
      } yield lessonId

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

    override def deleteLesson(id: UserId, lessonId: LessonId): Task[Unit] = {
      for {
        lessonOpt <- lessonRepository.get(lessonId)
        lesson <- ZIO.fromEither(lessonOpt.toRight(LessonNotFound))
        _ <- ZIO.when(lesson.completed)(ZIO.fail(DeletingCompletedLesson))
        _ <- ZIO.when(id != lesson.teacherId && id != lesson.studentId)(ZIO.fail(UnauthorizedAction))
        studentOpt <- studentRepository.get(lesson.studentId)
        student <- ZIO.fromEither(studentOpt.toRight(StudentNotFound))
        teacherOpt <- teacherRepository.get(lesson.teacherId)
        teacher <- ZIO.fromEither(teacherOpt.toRight(TeacherNotFound))
        updStudent = student.copy(
          balance = student.balance + teacher.rate,
          reserved = student.reserved - teacher.rate
        )
        _ <- lessonRepository.delete(updStudent, lessonId)
      } yield ()

    }
  }
}