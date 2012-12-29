package com.github.mumoshu.mmo.server.tcpip

import org.specs2.mutable._
import akka.actor.IO
import org.specs2.mock.Mockito
import akka.util.ByteString
import com.github.mumoshu.mmo.models.world.world.{StringIdentity, Position}

object RangedPublisherSpec extends Specification with Mockito {

  "RangedPublisher" should {

    // client n --send--> server --publish--> client1, client2, ...
    //
    // Each client is a socket handle with coordinates in the world
    "publish messages to sockets" in {
      val position = Position(1f, 1f)
      val bound = 2f
      val pub = RangedPublisher(bound = bound)
      val handle1 = mock[IO.SocketHandle]
      val handle2 = mock[IO.SocketHandle]
      val client1Observer = mock[PositionedClientObserver]
      val client2Observer = mock[PositionedClientObserver]
      val id1 = StringIdentity("id1")
      val id2 = StringIdentity("id2")
      val client1 = mock[Channel]
      val client2 = mock[Channel]

      implicit val any2ByteString = DefaultByteStringWriter

      pub.accept(id1, client1).accept(id2, client2).publish("foo")

      there was one(client1).write("foo") then
        one(client2).write("foo")

    }
  }

}
