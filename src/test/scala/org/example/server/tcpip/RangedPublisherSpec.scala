package org.example.server.tcpip

import org.specs2.mutable._
import akka.actor.IO
import org.specs2.mock.Mockito
import akka.util.ByteString
import org.example.models.world.world.Position

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
      val client1 = new PositionedClient(handle1, position, observer = client1Observer)
      val client2 = new PositionedClient(handle2, position, observer = client2Observer)

      implicit val any2ByteString = new ByteStringWriter {
        def apply(any: Any) = any match {
          case str: String =>
            ByteString(str)
          case unexpected =>
            throw new RuntimeException("Unsurppoted object: " + unexpected)
        }
      }

      pub.accept(client1).accept(client2).publish("foo").clients.forall { _.received must be size(1) }

      there was one(client1Observer).receive("foo") then
        one(client2Observer).receive("foo")

      there was one(handle1).write(ByteString("foo")) then
        one(handle2).write(ByteString("foo"))

    }
  }

}
