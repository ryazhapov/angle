package io.ryazhapov

import io.ryazhapov.InMemoryRepositories._
import io.ryazhapov.database.repositories.auth.UserRepository.UserRepository
import io.ryazhapov.domain.accounts.Role.{AdminRole, StudentRole, TeacherRole}
import io.ryazhapov.domain.auth.{SignUpRequest, User}
import io.ryazhapov.services.auth.UserService
import zio.ZIO
import zio.test.Assertion.{equalTo, hasField, isSome}
import zio.test._

object UserServiceTests extends DefaultRunnableSpec {
  override def spec = suite("user service tests")(
    testM("student can sign up") {

      val request = SignUpRequest("student@gmail.com", "password", StudentRole)
      for {
        response <- UserService.createUser(request)
        user <- UserService.getUser(response)
        saved <- ZIO.accessM[UserRepository](_.get.get(user.id))
      } yield assert(saved)(
        isSome(
          hasField("email", (e: User) => e.email, equalTo(request.email)) &&
            hasField(
              "role",
              (e: User) => e.role,
              equalTo(request.role)
            )
        )
      )
    }.provideSomeLayer(layer()),

    testM("teacher can sign up") {

      val request = SignUpRequest("teacher@gmail.com", "password", TeacherRole)
      for {
        response <- UserService.createUser(request)
        user <- UserService.getUser(response)
        saved <- ZIO.accessM[UserRepository](_.get.get(user.id))
      } yield assert(saved)(
        isSome(
          hasField("email", (e: User) => e.email, equalTo(request.email)) &&
            hasField(
              "role",
              (e: User) => e.role,
              equalTo(request.role)
            )
        )
      )
    }.provideSomeLayer(layer()),

    testM("admin can sign up") {

      val request = SignUpRequest("admin@gmail.com", "password", AdminRole)
      for {
        response <- UserService.createUser(request)
        user <- UserService.getUser(response)
        saved <- ZIO.accessM[UserRepository](_.get.get(user.id))
      } yield assert(saved)(
        isSome(
          hasField("email", (e: User) => e.email, equalTo(request.email)) &&
            hasField(
              "role",
              (e: User) => e.role,
              equalTo(request.role)
            )
        )
      )
    }.provideSomeLayer(layer()),
  )



  private def layer() = {
    val userRepo = InMemoryUserRepository.live()
    val sessionRepo = InMemorySessionRepository.live()
    val studentRepo = InMemoryStudentRepository.live()
    val teacherRepo = InMemoryTeacherRepository.live()
    val adminRepo = InMemoryAdminRepository.live()
    userRepo >+> sessionRepo >+> studentRepo >+>
      teacherRepo >+> adminRepo >+>
      UserService.live
  }
}
