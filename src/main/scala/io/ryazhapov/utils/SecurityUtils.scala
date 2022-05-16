package io.ryazhapov.utils

import org.apache.commons.codec.digest.DigestUtils

import java.security.SecureRandom

object SecurityUtils {
  def countSecretHash(secret: String, salt: Array[Byte]): String =
    DigestUtils.sha256Hex(secret.getBytes ++ salt)

  def checkSecret(secret: String, salt: Array[Byte], passwordHash: String): Boolean =
    DigestUtils.sha256Hex(secret.getBytes ++ salt) == passwordHash

  def generateSalt(length: Int = 32): Array[Byte] = {
    val salt = Array.fill(length)(0.byteValue())
    new SecureRandom().nextBytes(salt)
    salt
  }
}