package org.example.server

import akka.actor._
import ActorDSL._
import java.net.InetSocketAddress
import org.example.protocol.Protocol
import akka.util.ByteString
import org.example.models.{Tile, Terrain, Id}
import org.example.models.world.world.{StringIdentity, Position, LivingPlayer, InMemoryWorld}

/**
 * See http://stackoverflow.com/questions/12959709/send-a-tcp-ip-message-akka-actor
 * and http://doc.akka.io/docs/akka/snapshot/scala/io.html
 */
object TCPIPServer {
  implicit val sys = ActorSystem("telnet")

  val protocol = new Protocol {

    type Payload = ByteString

    val codec = new Codec[Payload] {
      /**
       * Decompose the TransportMessage and extracts its content
       * @param m the message decomposed
       * @return
       */
      def unapply(m: ByteString) = {
        val (hint, frames) = m.splitAt(1)
        Some((hint(0), frames.toArray))
      }

      /**
       * Composes the message content into a TransportMessage
       * @param hint
       * @param bytes
       * @return
       */
      def apply(hint: Byte, bytes: Array[Byte]) =
        ByteString(Array(hint) ++ bytes : _*)
    }
  }

  var world = new InMemoryWorld(List.empty, Terrain(Array(Array(Tile.Ground, Tile.Ground))), List.empty)

  val server = actor(new Act with ActorLogging {
    println("Starting")
    val socket = IOManager(context.system) listen new InetSocketAddress(1234)
    become {
      case IO.NewClient(server) ⇒
        val clientSocket = server.accept()
        println("Accepted: " + clientSocket.uuid.toString)
        // You need this to read incoming packets afterwards
        // Unfortunately I don't know why
        clientSocket.write(ByteString("You are connected"))
      case IO.Read(handle, bytes) ⇒
        import protocol._
        println("Read: " + handle.uuid)
        println("Message: " + bytes)
        val identity = handle
        val id = StringIdentity(handle.uuid.toString)
        deserialize(bytes) match {
          case m: serializers.thrift.Join =>
            world = world.join(new LivingPlayer(id, 10f, Position(0f, 0f)))
            world.findExcept(id).foreach { p =>
              //
            }
        }
      case "STOP" =>
        println("Closing the server socket")
        socket.close()
      case unexpected =>
        println("Unexpected message: " + unexpected)
      }
  })
}
