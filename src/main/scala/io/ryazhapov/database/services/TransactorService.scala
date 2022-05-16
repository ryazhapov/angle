package io.ryazhapov.database.services

import cats.effect.Blocker
import doobie.hikari.HikariTransactor
import doobie.quill.DoobieContext
import doobie.util.transactor.Transactor
import io.getquill.{Escape, Literal, NamingStrategy}
import io.ryazhapov.config.ConfigService.Configuration
import io.ryazhapov.config.{Config, DatabaseConfig}
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Has, Managed, Task, URIO, ZIO, ZLayer}

import scala.concurrent.ExecutionContext

object TransactorService {

  type DBTransactor = Has[Transactor[Task]]

  lazy val doobieContext = new DoobieContext.Postgres(NamingStrategy(Escape, Literal))

  def createTransactor(
    conf: DatabaseConfig,
    connectEC: ExecutionContext,
    transactEC: ExecutionContext): Managed[Throwable, Transactor[Task]] =
    HikariTransactor.newHikariTransactor[Task](
      conf.driver,
      conf.url,
      conf.user,
      conf.password,
      connectEC,
      Blocker.liftExecutionContext(transactEC)
    ).toManagedZIO

  def databaseTransactor: URIO[DBTransactor, Transactor[Task]] = ZIO.service[Transactor[Task]]

  lazy val live: ZLayer[Configuration with Blocking, Throwable, DBTransactor] = ZLayer.fromManaged(
    for {
      config <- zio.config.getConfig[Config].toManaged_
      connectEC <- ZIO.descriptor.map(_.executor.asEC).toManaged_
      blockingEC <- zio.blocking.blockingExecutor.map(_.asEC).toManaged_
      transactor <- TransactorService.createTransactor(config.database, connectEC, blockingEC)
    } yield transactor
  )
}
