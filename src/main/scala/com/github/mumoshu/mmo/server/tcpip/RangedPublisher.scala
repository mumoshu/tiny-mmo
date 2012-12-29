package com.github.mumoshu.mmo.server.tcpip

import org.slf4j.LoggerFactory
import com.github.mumoshu.mmo.models.world.world.{Position, Identity}
import akka.actor.{IO, ActorRef}
import com.github.mumoshu.mmo.server.TCPIPServer

sealed trait Channel {
  def write(message: AnyRef)
}

case class ActorChannel(ref: ActorRef) extends Channel {
  def write(message: AnyRef) {
    ref ! message
  }
}

case class SocketChannel(handle: IO.SocketHandle, any2ByteString: ByteStringWriter) extends Channel {
  def write(message: AnyRef) {
    handle.write(TCPIPServer.FrameEncoder(any2ByteString(message)))
  }
}

case class RangedPublisher(bound: Float, clients: List[PositionedClient] = List.empty, channels: Map[Identity, Channel] = Map.empty) {

  val log = LoggerFactory.getLogger(classOf[RangedPublisher])

  def accept(id: Identity, c: Channel): RangedPublisher = copy(channels = channels.updated(id, c))
  def remove(id: Identity): RangedPublisher = copy(channels = channels.filterKeys(_ != id))
//  def publishRange[T <: Any](id: Identity, data: T)(implicit any2ByteString: ByteStringWriter, positionProvider: Identity => Position): RangedPublisher =


  def accept(c: PositionedClient): RangedPublisher = copy(clients = clients :+ c)
  def remove(c: PositionedClient): RangedPublisher = copy(clients = clients.filter(_ != c))

  def publish[T <: AnyRef](data: T)(implicit any2ByteString: ByteStringWriter): RangedPublisher = {
    log.debug("Publish: " + data)
    channels.foreach(_._2.write(data))
    this
  }
//    copy(clients = clients.map(c => {
//      log.debug("Publishing: " + data)
//      c.receive(data)
//    }))

}
