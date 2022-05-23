package io.ryazhapov.database.services

import doobie.quill.DoobieContext
import doobie.util.transactor.Transactor
import io.getquill.{Escape, Literal, NamingStrategy}
import io.ryazhapov.config.ConfigService.Configuration
import io.ryazhapov.config.{Config, DatabaseConfig}
import zio.interop.catz._
import zio.{Has, Task, URIO, ZIO, ZLayer}

object TransactorService {

  type DBTransactor = Has[Transactor[Task]]

  lazy val doobieContext = new DoobieContext.Postgres(NamingStrategy(Escape, Literal))
  lazy val live: ZLayer[Configuration, Throwable, DBTransactor] = (
    for {
      config <- zio.config.getConfig[Config]
      transactor = TransactorService.createTransactor(config.database)
    } yield transactor).toLayer

  def createTransactor(conf: DatabaseConfig) =
    Transactor.fromDriverManager[Task](
      conf.driver,
      conf.url,
      conf.user,
      conf.password
    )

  def databaseTransactor: URIO[DBTransactor, Transactor[Task]] = ZIO.service[Transactor[Task]]
}
