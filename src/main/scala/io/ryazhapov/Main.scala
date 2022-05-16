package io.ryazhapov

import io.ryazhapov.server.Server
import zio.{App, ExitCode, URIO, ZEnv}

object Main extends App {
  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    Server.start()
}
