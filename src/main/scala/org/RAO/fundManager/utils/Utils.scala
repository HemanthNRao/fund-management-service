package org.RAO.fundManager.utils

import java.security.MessageDigest
import scala.util.Random

object Utils
{
  def toHex(bytes: Array[Byte]): String = bytes.map(0xFF & _).map("%02x".format(_)).mkString("")
  def md5hash(string: String): String = toHex(MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8")))
  def constructRandomKey(length: Int = 30): String = md5hash(Random.nextString(length)).substring(0, length)
}
