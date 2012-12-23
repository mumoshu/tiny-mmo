package org.example.server.tcpip

trait PositionedClientObserver {

   def receive[T](data: T)(implicit any2ByteString: ByteStringWriter)
 }
