package org.example.server

import org.specs2.mutable._
import java.net.InetSocketAddress
import akka.actor._
import akka.pattern._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.{ByteString, Timeout}

class Client extends Actor {
  val address = new InetSocketAddress("localhost", 1234)
  val socket = IOManager(context.system).connect(address)
  val state = IO.IterateeRef.Map.async[IO.Handle]()(context.dispatcher)
  var sock: Option[IO.SocketHandle] = None
  var oldSender: Option[ActorRef] = None
  println("Starting with socket " + socket.uuid)

  def receive = {
    case IO.Connected(socket, address) =>
      println("Connected to server with the handle: " + socket.uuid)
      sock = Some(socket)
    case IO.Read(socket, bytes) =>
      println("Read" + bytes.toString)
    case bytes: ByteString =>
      println("Sending")
      oldSender = Some(sender)
      sock.foreach { s =>
        println("To server with the handle: " + s.uuid)
        s.write(bytes.compact)
        s.close()
        println(bytes.length + " bytes sent")
      }
      oldSender.foreach { s => s ! true }
  }
}

object Tester {

  def test = {
    val system = ActorSystem("client")
    val server = TCPIPServer.server
    Thread.sleep(1100)
    val client = system.actorOf(Props[Client])
    Thread.sleep(1100)
    implicit val timeout = Timeout(5 seconds)
    val bytes = TCPIPServer.protocol.serialize(new serializers.thrift.Join("mumoshu"))
    var r: Option[Boolean] = None
    try {
      val result = Await.result((client ? bytes.asInstanceOf[ByteString]).mapTo[Boolean], timeout.duration)
      r = Some(result)
    } catch {
      case e =>
      r = None
    } finally {
      server ! "STOP"
    }
    r
  }
}

object TCPIPServerSpec extends Specification {

  "TCPIPServer" should {

    "accept joining user" in {

      val result = Tester.test
      result must be some

      Thread.sleep(1100)

      TCPIPServer.world.things must be size(1)
    }
  }
}
