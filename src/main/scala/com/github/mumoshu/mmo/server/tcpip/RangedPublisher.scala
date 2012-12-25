package com.github.mumoshu.mmo.server.tcpip

import org.slf4j.LoggerFactory

case class RangedPublisher(bound: Float, clients: List[PositionedClient] = List.empty) {

  val log = LoggerFactory.getLogger(classOf[RangedPublisher])

  def accept(c: PositionedClient): RangedPublisher = copy(clients = clients :+ c)
  def remove(c: PositionedClient): RangedPublisher = copy(clients = clients.filter(_ != c))

  def publish[T <: Any](data: T)(implicit any2ByteString: ByteStringWriter): RangedPublisher =
    copy(clients = clients.map(c => {
      log.debug("Publishing: " + data)
      c.receive(data)
    }))

}
