package io.ryazhapov

package object errors {

  abstract class AppError(val message: String) extends Throwable(message)

  case class  UserNotExist(email: String) extends AppError(s"User with name=$email does not exist")
  case object UserNotFound extends AppError("User not found for session")

  case object SessionCookieIsAbsent extends AppError("Session cookie is absent")
  case object SessionNotFound extends AppError("Session not found")
  case object IncorrectUserPassword extends AppError("User password is incorrect")

  case object AdminNotFound extends AppError("Admin not found")
  case object TeacherNotFound extends AppError("Teacher not found")
  case object StudentNotFound extends AppError("Student not found")
  case object ScheduleNotFound extends AppError("Schedule not found")

  case object UserAlreadyExists extends AppError("User already exists")
  case object TeacherAlreadyExists extends AppError("Teacher already exists")
  case object StudentAlreadyExists extends AppError("Student already exists")

  case object ScheduleOverlapping extends AppError("Schedule is overlapping another schedule")
  case object ScheduleAlreadyExists extends AppError("Schedule already exists")
  case object InvalidScheduleTime extends AppError("Invalid schedule time: Duration is zero or negative")
}
