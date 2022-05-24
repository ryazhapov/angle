package io.ryazhapov.repositories

import io.ryazhapov.repositories.maps.scheduleMap
import io.ryazhapov.database.repositories.lessons.ScheduleRepository
import io.ryazhapov.database.repositories.lessons.ScheduleRepository.ScheduleRepository
import io.ryazhapov.domain.lessons.Schedule
import io.ryazhapov.domain.{ScheduleId, UserId}
import zio.{Task, ZLayer}

import java.time.ZonedDateTime

class InMemoryScheduleRepository extends ScheduleRepository.Service {

  import ZonedDateTimeExtension._

  override def create(schedule: Schedule): Task[ScheduleId] = Task.succeed {
    scheduleMap.put(schedule.id, schedule)
    schedule.id
  }

  override def update(schedule: Schedule): Task[Unit] = Task.succeed {
    scheduleMap.update(schedule.id, schedule)
  }

  override def get(id: ScheduleId): Task[Option[Schedule]] = Task.succeed {
    scheduleMap.get(id)
  }

  override def getAll: Task[List[Schedule]] = Task.succeed {
    scheduleMap.values.toList
  }

  override def getByTeacher(teacherId: UserId): Task[List[Schedule]] = Task.succeed {
    scheduleMap.values.find(_.teacherId == teacherId).toList
  }

  override def findIntersection(lessonStart: ZonedDateTime, lessonEnd: ZonedDateTime): Task[List[Schedule]] = Task.succeed {
    scheduleMap.values.filter(x => !(x.startsAt isAfter lessonStart) && !(x.endsAt isBefore lessonEnd)).toList
  }

  override def findUnion(id: UserId, start: ZonedDateTime, end: ZonedDateTime): Task[List[Schedule]] = Task.succeed {
    scheduleMap.values.filter(_.teacherId == id)
      .filter(x =>
        !(x.startsAt > start && x.startsAt >= x.endsAt) &&
          !(x.endsAt <= start && x.endsAt < end)).toList
  }

  override def delete(id: ScheduleId): Task[Unit] = Task.succeed {
    scheduleMap.remove(id)
  }
}

object InMemoryScheduleRepository {
  def live(): ZLayer[Any, Nothing, ScheduleRepository] =
    ZLayer.succeed(new InMemoryScheduleRepository)
}