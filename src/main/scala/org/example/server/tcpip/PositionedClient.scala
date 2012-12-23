package org.example.server.tcpip

import akka.actor.IO
import org.example.models.world.world.Position

case class PositionedClient(handle: IO.SocketHandle, position: Position, received: List[Any] = List.empty, observer: PositionedClientObserver = NullPositionedClientObserver) {
   def receive[T](data: T)(implicit any2ByteString: ByteStringWriter): PositionedClient = {
     println("Received " + data)
     observer.receive(data)
     handle.write(any2ByteString(data))
     copy(received = received :+ data)
   }
 }
