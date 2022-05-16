package io.ryazhapov

package object errors {

  abstract class Error(val message: String) extends Throwable(message)

  case class  UserNotExist(email: String) extends Error(s"User with name=$email does not exist")
  case object UserNotFound extends Error("User not found for session")

  case object SessionCookieIsAbsent extends Error("Session cookie is absent")
  case object SessionNotFound extends Error("Session not found")
  case object IncorrectUserPassword extends Error("User password is incorrect")

  case object AdminNotFound extends Error("Admin not found")
  case object TeacherNotFound extends Error("Teacher not found")
  case object StudentNotFound extends Error("Student not found")

  case object UserAlreadyExists extends Error("User already exists")
  case object TeacherAlreadyExists extends Error("Teacher already exists")
  case object StudentAlreadyExists extends Error("Student already exists")

}
