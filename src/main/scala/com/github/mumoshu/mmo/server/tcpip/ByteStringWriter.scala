package com.github.mumoshu.mmo.server.tcpip

import akka.util.ByteString

trait ByteStringWriter {
   def apply(any: Any): ByteString
 }
