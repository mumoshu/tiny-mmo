package org.example.server.tcpip

import akka.actor.IO
import org.example.models.world.world.Position
import org.slf4j.LoggerFactory
import org.example.server.TCPIPServer.FrameEncoder

case class PositionedClient(handle: IO.SocketHandle, position: Position, received: List[Any] = List.empty, observer: PositionedClientObserver = NullPositionedClientObserver) {
  
  val log = LoggerFactory.getLogger(classOf[PositionedClient])
  
   def receive[T](data: T)(implicit any2ByteString: ByteStringWriter): PositionedClient = {
     log.debug("Received: " + data + ", " + data.getClass)
     observer.receive(data)
     handle.write(FrameEncoder(any2ByteString(data)))
     log.debug("Wrote " + data + " via Handle " + handle)
     copy(received = received :+ data)
   }
 }
