package com.github.mumoshu.mmo.server.tcpip

case object NullPositionedClientObserver extends PositionedClientObserver {
   def receive[T](data: T)(implicit any2ByteString: ByteStringWriter) {}
 }
