package io.ryazhapov

package object errors {

  abstract class AppError(val message: String) extends Throwable(message)

  case class UserNotExist(email: String) extends AppError(s"User with name=$email does not exist")

  case object UserNotFound extends AppError("User not found for session")

  case object SessionCookieIsAbsent extends AppError("Session cookie is absent")

  case object SessionNotFound extends AppError("Session not found")

  case object IncorrectUserPassword extends AppError("User password is incorrect")

  case object AdminNotFound extends AppError("Admin not found")

  case object TeacherNotFound extends AppError("Teacher not found")

  case object StudentNotFound extends AppError("Student not found")

  case object ScheduleNotFound extends AppError("Schedule not found")

  case object LessonNotFound extends AppError("Lesson not found")

  case object ReplenishmentNotFound extends AppError("Replenishment not found")

  case object PaymentNotFound extends AppError("Payment not found")

  case object WithdrawalNotFound extends AppError("Withdrawal not found")

  case object UserAlreadyExists extends AppError("User already exists")

  case object TeacherAlreadyExists extends AppError("Teacher already exists")

  case object StudentAlreadyExists extends AppError("Student already exists")

  case object LessonOverlapping extends AppError("Lesson is overlapping another lesson")

  case object ScheduleOverlapping extends AppError("Schedule is overlapping another schedule")

  case object ScheduleAlreadyExists extends AppError("Schedule already exists")

  case object InvalidScheduleTime extends AppError("Invalid schedule time: Duration is zero or negative")

  case object InvalidLessonTime extends AppError("Invalid lesson time: Duration is zero or negative")

  case object UnauthorizedAction extends AppError("Performing unauthorized actions")

  case object DeletingCompletedLesson extends AppError("Deleting already completed lesson")

  case object NotEnoughMoney extends AppError("Not enough money on account to perform action")
}
