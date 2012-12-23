package org.example.server.tcpip

case class RangedPublisher(bound: Float, clients: List[PositionedClient] = List.empty) {
   def accept(c: PositionedClient): RangedPublisher = copy(clients = clients :+ c)
   def publish[T <: Any](data: T)(implicit any2ByteString: ByteStringWriter): RangedPublisher =
     copy(clients = clients.map(c => {println(data); c.receive(data)}))

 }
