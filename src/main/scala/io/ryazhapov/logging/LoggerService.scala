package io.ryazhapov.logging

import zio.clock.Clock
import zio.console.Console
import zio.logging.{LogFormat, LogLevel, Logging}
import zio.{Has, RLayer}

object LoggerService {

  type LoggerService = Has[zio.logging.Logger[String]]

  val live: RLayer[Console with Clock, LoggerService] =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("ANGLE")
}
