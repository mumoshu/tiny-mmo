package org.example.server.tcpip

import akka.util.ByteString

trait ByteStringWriter {
   def apply(any: Any): ByteString
 }
