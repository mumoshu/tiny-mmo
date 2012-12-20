package org.example.server

import akka.actor._
import ActorDSL._
import java.net.InetSocketAddress
import org.example.protocol.Protocol
import akka.util.ByteString

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

  actor(new Act with ActorLogging {
    println("Starting")
    IOManager(context.system) listen new InetSocketAddress(1234)
    become {
      case IO.NewClient(server) ⇒
        server.accept()
      case IO.Read(handle, bytes) ⇒
        log.info("got {} from {}", bytes.decodeString("utf-8"), handle)
      }
  })
}
