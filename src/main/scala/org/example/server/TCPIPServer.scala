package org.example.server

import akka.actor._
import ActorDSL._
import java.net.InetSocketAddress
import org.example.protocol.Protocol
import akka.util.ByteString
import org.example.models.{Tile, Terrain, Id}
import org.example.models.world.world.{StringIdentity, Position, LivingPlayer, InMemoryWorld}
import tcpip.{DefaultByteStringWriter, PositionedClient, RangedPublisher}
import scala.concurrent.stm._
import akka.agent.Agent
import java.util
import serializers.thrift

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

  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

  val FrameDecoder: IO.Iteratee[ByteString] = for {
    frameLenBytes <- IO.take(4)
    frameLen = frameLenBytes.iterator.getInt
    frame <- IO.take(frameLen)
  } yield {
//    val in = frame.iterator
//    val data = Array.ofDim[Byte](in)
    frame
  }

  object FrameEncoder {
    def apply(bytes: Array[Byte]): ByteString = {
      val builder = ByteString.newBuilder
      builder.putInt(bytes.length)
      builder.putBytes(bytes)
      builder.result()
    }
    def apply(bytes: ByteString): ByteString = {
      apply(bytes.toArray)
    }
  }

  def createServer(port: Int = 1234) = actor(new Act with ActorLogging {

    val world = Agent(new InMemoryWorld(List.empty, Terrain(Array(Array(Tile.Ground, Tile.Ground))), List.empty))
    val publisher = Agent(RangedPublisher(10f))
    val state = IO.IterateeRef.Map.async[IO.Handle]()(context.dispatcher)

    implicit val any2ByteString = DefaultByteStringWriter

    log.debug("Starting server")

    def publish(m: AnyRef) = {
      publisher.get().publish(m)
    }

    val address = new InetSocketAddress(port)
    val socket = IOManager(context.system) listen address

    log.debug("Now listening on " + address)

    def processSingle(handle: IO.SocketHandle, bytes: ByteString) {
      import protocol._
      log.debug("Read: " + handle.uuid)
      log.debug("Message: " + bytes + " (" + bytes.length + " bytes)")
      val identity = handle
      val id = StringIdentity(handle.uuid.toString)
      val deserialized = deserialize(bytes)
      log.debug("Server deserialized the message: " + deserialized)
      atomic { txn =>
        log.debug("Beginning a transaction")
        deserialized match {
          case m: serializers.thrift.Join =>
            world send {
              _.join(new LivingPlayer(id, 10f, Position(0f, 0f)))
            }
            publisher send {
              _.accept(PositionedClient(handle.asSocket, Position(0f, 0f))).publish(m)
            }
          case m: serializers.thrift.Leave =>
            world.send {
              _.leave(new LivingPlayer(id, 0f, Position(0f, 0f)))
            }
            // TODO Use STM
            publisher.send { pub =>
              pub.clients.filter(_.handle.uuid == handle.asSocket.uuid).foldLeft(pub) { (res, c) =>
                res.remove(c)
              }.publish(m)
            }
          case m: serializers.thrift.MoveTo =>
            // TODO you only need id
            world.send {
              _.tryMoveTo(new LivingPlayer(id, 10f, Position(0f, 0f)), Position(m.x.toFloat, m.z.toFloat))._1
            }
            publish(m)
          case m: serializers.thrift.Attack =>
            // TODO attack if the thing identified by id can be an attacker
            world.send {
              _.tryAttack(id, StringIdentity(m.targetId))._1
            }
            publish(m)
          case m: serializers.thrift.Say =>
            world.send {
              _.trySay(id, m.text)
            }
            publish(m)
          case m: serializers.thrift.Shout =>
            world.send {
              _.tryShout(id, m.text)
            }
            publish(m)
          case m: serializers.thrift.MyId =>
            val data = protocol.serialize(new serializers.thrift.YourId(id.str))
            handle.asWritable.write(FrameEncoder(data))
          case m: serializers.thrift.GetPosition =>
            val targetId = StringIdentity(m.id)
            val p = world.get().things.find(_.id == targetId).get.position
            val rep: thrift.Position = new serializers.thrift.Position(targetId.str, p.x.toFloat, p.z.toFloat)
            val data = protocol.serialize(rep)
            handle.asWritable.write(FrameEncoder(data))
            log.debug("Wrote: " + rep)
          case m: serializers.thrift.FindAllThings =>
            val things = new serializers.thrift.Things()
            things.things = new java.util.ArrayList[serializers.thrift.Thing]()
            world.get().things.foreach { t =>
            // Things other than the client are included
              if (t.id != id) {
                val p = new serializers.thrift.Position(t.id.str, t.position.x.toDouble, t.position.z.toDouble)
                things.things.add(new serializers.thrift.Thing(t.id.str, p))
              }
            }
            val data = protocol.serialize(things)
            handle.asWritable.write(FrameEncoder(data))
        }
      }
    }

    def processData(handle: IO.SocketHandle): IO.Iteratee[Unit] = {
      IO repeat {
        for {
          bytes <- FrameDecoder
        } yield {
          processSingle(handle, bytes)
        }
      }
    }

    become {
      case IO.NewClient(server) ⇒
        val socket = server.accept()
        log.debug("Accepted: " + socket.uuid.toString)
        // You need this to read incoming packets afterwards
        // Unfortunately I don't know why
        socket.write(FrameEncoder(ByteString("You are connected")))
        state(socket) flatMap (_ => processData(socket))
      case IO.Read(handle, bytes) ⇒
        state(handle)(IO Chunk bytes)
      case IO.Closed(socket, cause) =>
        log.debug("Socket closed: " + cause)
        state(socket)(IO EOF)
        state -= socket
      case "STOP" =>
        log.debug("Closing the server socket")
        socket.close()
      case "WORLD" =>
        sender ! world.get
      case unexpected =>
        log.debug("Unexpected message: " + unexpected)
    }
  })

  lazy val server = createServer(1234)
}
