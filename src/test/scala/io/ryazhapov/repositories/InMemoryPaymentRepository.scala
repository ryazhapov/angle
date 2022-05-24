package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.{lessonMap, paymentMap, studentMap, teacherMap}
import io.ryazhapov.database.repositories.billing.PaymentRepository
import io.ryazhapov.database.repositories.billing.PaymentRepository.PaymentRepository
import io.ryazhapov.domain.accounts.{Student, Teacher}
import io.ryazhapov.domain.billing.Payment
import io.ryazhapov.domain.lessons.Lesson
import io.ryazhapov.domain.{LessonId, PaymentId, UserId}
import zio.{Task, ZLayer}

class InMemoryPaymentRepository extends PaymentRepository.Service {

  override def create(student: Student, teacher: Teacher, lesson: Lesson, payment: Payment): Task[PaymentId] =
    Task.succeed {
      studentMap.update(student.userId, student)
      teacherMap.update(teacher.userId, teacher)
      lessonMap.update(lesson.id, lesson)
      paymentMap.put(payment.id, payment)
      payment.id
    }

  override def get(id: PaymentId): Task[Option[Payment]] = Task.succeed {
    paymentMap.get(id)
  }

  override def getAll: Task[List[Payment]] = Task.succeed {
    paymentMap.values.toList
  }

  override def getByStudent(studentId: UserId): Task[List[Payment]] = Task.succeed {
    paymentMap.values.find(_.studentId == studentId).toList
  }

  override def getByTeacher(teacherId: UserId): Task[List[Payment]] = Task.succeed {
    paymentMap.values.find(_.teacherId == teacherId).toList
  }

  override def getByLesson(lessonId: LessonId): Task[Option[Payment]] = Task.succeed {
    paymentMap.values.find(_.lessonId == lessonId)
  }
}

object InMemoryPaymentRepository {
  def live(): ZLayer[Any, Nothing, PaymentRepository] =
    ZLayer.succeed(new InMemoryPaymentRepository)
}