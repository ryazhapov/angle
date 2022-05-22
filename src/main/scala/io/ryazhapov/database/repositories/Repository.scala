package io.ryazhapov.database.repositories

import doobie.quill.DoobieContext
import io.getquill.{CompositeNamingStrategy2, Escape, Literal, SnakeCase}
import io.ryazhapov.database.services.TransactorService

import java.sql.{Timestamp, Types}
import java.time.{ZoneId, ZonedDateTime}

trait Repository {
  val dbContext: DoobieContext.Postgres[CompositeNamingStrategy2[Escape.type, Literal.type]] =
    TransactorService.doobieContext

  import dbContext._

  class ExtendedResult[T](val result: Result[T]) {
    def unit: Result[Unit] = result.map(_ => ())
  }

  implicit def resultToUnit[T](result: Result[T]): ExtendedResult[T] = new ExtendedResult(result)


  implicit val zoneDateTimeEncoder: Encoder[ZonedDateTime] =
    encoder(Types.TIMESTAMP_WITH_TIMEZONE, (index, value, row) =>
      row.setTimestamp(index, Timestamp.from(value.toInstant)))

  implicit val zoneDateTimeDecoder: Decoder[ZonedDateTime] = decoder((index, row) => {
    ZonedDateTime.ofInstant(row.getTimestamp(index).toInstant, ZoneId.of("UTC"))
  })


  // don't add type annotations, app won't compile, reason: https://github.com/zio/zio-quill/issues/1406
  implicit class ZonedDateTimeQuotes(left: ZonedDateTime) {
    def >(right: ZonedDateTime) = quote(infix"$left > $right".as[Boolean])

    def <(right: ZonedDateTime) = quote(infix"$left < $right".as[Boolean])

    def >=(right: ZonedDateTime) = quote(infix"$left >= $right".as[Boolean])

    def <=(right: ZonedDateTime) = quote(infix"$left <= $right".as[Boolean])
  }
}
