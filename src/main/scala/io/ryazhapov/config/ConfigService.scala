package io.ryazhapov.config

import zio.config.ReadError
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfig
import zio.{Has, Layer}

object ConfigService {

  type Configuration = Has[Config]

  lazy val live: Layer[ReadError[String], Configuration] = {
    val configDescriptor = descriptor[Config]
    TypesafeConfig.fromDefaultLoader(configDescriptor)
  }
}
