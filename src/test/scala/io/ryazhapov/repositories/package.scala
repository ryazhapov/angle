package io.ryazhapov

import io.ryazhapov.domain.accounts.{Admin, Student, Teacher}
import io.ryazhapov.domain.auth.User
import io.ryazhapov.domain.billing.{Payment, Replenishment, Withdrawal}
import io.ryazhapov.domain.lessons.{Lesson, Schedule}

import java.time.ZonedDateTime
import scala.collection.concurrent.TrieMap

package object repositories {

  object ZonedDateTimeExtension {
    class ZonedDateTimeExtension(left: ZonedDateTime) {

      def >(right: ZonedDateTime): Boolean = left.isAfter(right)

      def >=(right: ZonedDateTime): Boolean = left.isAfter(right) || left.isEqual(right)

      def <(right: ZonedDateTime): Boolean = left.isBefore(right)

      def <=(right: ZonedDateTime): Boolean = left.isBefore(right) || left.isEqual(right)
    }

    implicit def extendZonedDateTime(a: ZonedDateTime) = new ZonedDateTimeExtension(a)
  }

  object maps {
    val adminMap = new TrieMap[Int, Admin]()
    val lessonMap = new TrieMap[Int, Lesson]()
    val paymentMap = new TrieMap[Int, Payment]()
    val replenishMap = new TrieMap[Int, Replenishment]()
    val studentMap = new TrieMap[Int, Student]()
    val scheduleMap = new TrieMap[Int, Schedule]()
    val sessionMap = new TrieMap[Int, Int]()
    val teacherMap = new TrieMap[Int, Teacher]()
    val userMap = new TrieMap[Int, User]()
    val withdrawalMap = new TrieMap[Int, Withdrawal]()
  }
}
