package org.example.server.tcpip

case object NullPositionedClientObserver extends PositionedClientObserver {
   def receive[T](data: T)(implicit any2ByteString: ByteStringWriter) {}
 }
