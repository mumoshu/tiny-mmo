package com.github.mumoshu.mmo.server.tcpip

import akka.util.ByteString
import com.github.mumoshu.mmo.server.TCPIPServer

object DefaultByteStringWriter extends ByteStringWriter {
  val protocol = TCPIPServer.protocol
  def apply(any: Any) = any match {
    case str: String =>
      ByteString(str)
    case anyRef: AnyRef =>
      protocol.serialize(anyRef)
    case unexpected =>
      throw new RuntimeException("Unsurppoted object: " + unexpected)
  }
}
