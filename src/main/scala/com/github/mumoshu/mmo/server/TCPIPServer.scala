package com.github.mumoshu.mmo.server

import akka.actor._
import ActorDSL._
import java.net.InetSocketAddress
import com.github.mumoshu.mmo.protocol.Protocol
import akka.util.ByteString
import com.github.mumoshu.mmo.models._
import com.github.mumoshu.mmo.models.world.world._
import tcpip._
import scala.concurrent.stm._
import akka.agent.Agent
import java.util
import com.github.mumoshu.mmo.thrift
import org.slf4j.LoggerFactory
import org.apache.thrift.TBase
import com.github.mumoshu.mmo.models.world.world.Position
import scala.Some
import com.github.mumoshu.mmo.models.world.world.StringIdentity
import com.github.mumoshu.mmo.server.WorldServer
import com.github.mumoshu.mmo.models.world.world.LivingPlayer
import com.github.mumoshu.mmo.models.world.world.Position
import tcpip.ActorChannel
import tcpip.RangedPublisher
import com.github.mumoshu.mmo.server.Change
import com.github.mumoshu.mmo.server.ReceivedLetter
import com.github.mumoshu.mmo.server.WorldServer
import scala.Some
import com.github.mumoshu.mmo.models.world.world.StringIdentity
import com.github.mumoshu.mmo.models.world.world.LivingPlayer
import com.github.mumoshu.mmo.models.world.world.InMemoryWorld
import tcpip.RangedPublisher
import tcpip.SocketChannel
import thrift.message.{Attack, MoveTo, Leave, Join}
import scala.Some
import world.world.InMemoryWorld
import world.world.LivingPlayer
import world.world.Position
import com.github.mumoshu.mmo.models.Terrain
import com.github.mumoshu.mmo.server.Change
import com.github.mumoshu.mmo.server.ReceivedLetter
import com.github.mumoshu.mmo.server.WorldServer
import world.world.StringIdentity

/**
 * See http://stackoverflow.com/questions/12959709/send-a-tcp-ip-message-akka-actor
 * and http://doc.akka.io/docs/akka/snapshot/scala/io.html
 */

object TCPIPServer extends TCPIPServer

// TODO Rename to AkkaWorldServer
class TCPIPServer {

  val log = LoggerFactory.getLogger(classOf[TCPIPServer])

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
    _ = log.debug("frameLen: " + frameLen)
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

//    val world = Agent(new InMemoryWorld(List.empty, Terrain(Array(Array(Tile.Ground, Tile.Ground))), List.empty, changeLogger))
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

    val server = Agent(new WorldServer(
      new InMemoryWorld(List.empty, Terrain(Array(Array(Tile.Ground, Tile.Ground))), List.empty, changeLogger)
    ))

    // TODO Agent
    var channels = Map.empty[Identity, Channel]

    def setChannel(id: Identity, channel: Channel) {
      atomic { txn =>
        channels = channels.updated(id, channel)
      }
    }

    def getChannel(id: Identity) = channels(id)

    def processSingle(handle: IO.SocketHandle, bytes: ByteString) {
      import protocol._
      log.debug("Read: " + handle.uuid)
      log.debug("Message: " + bytes + " (" + bytes.length + " bytes)")
      val identity = handle
      val id = StringIdentity(handle.uuid.toString)
      val deserialized = deserialize(bytes)
      log.debug("WorldServer deserialized the message: " + deserialized)
      setChannel(id, SocketChannel(handle, any2ByteString))
      receiveLetter(ReceivedLetter(id, deserialized))
    }

    lazy val changeLogger = MyChangeLogger(publisher)

    case class MyChangeLogger(publisher: Agent[RangedPublisher], changeLogs: List[ChangeLog] = List.empty) extends ChangeLogger {
      def changeLog(block: => Unit): ChangeLogger = copy(changeLogs = changeLogs :+ ChangeLog(block))
      def replay() = {
        changeLogs.foreach(_.replay())
        copy(changeLogs = List.empty)
      }
      def joined(id: Identity, name: String) = changeLog {
        val m = new Join()
        m.name = name
        publisher send {
          //                _.accept(PositionedClient(getChannel(id), Position(0f, 0f))).publish(m)
          //                val positionProvider = { id: Identity => server().world.find(id).get.position }
          //                _.accept(id, getChannel(id)).publish(m)(positionProvider)
          _.accept(id, getChannel(id)).publish(m)
        }

      }
      def left(id: Identity) = changeLog {
        publisher.send { pub =>
          val m = new Leave()
          pub.remove(id).publish(m)
          //                pub.clients.filter(_.handle.uuid == handle.asSocket.uuid).foldLeft(pub) { (res, c) =>
          //                  res.remove(c)
          //                }.publish(m)
        }
      }
      def movedTo(id: Identity, position: Position) = changeLog {
        val m = new MoveTo(id.str, position.x, position.z)
        publish(m)
      }
      def attacked(id: Identity, targetId: Identity) = changeLog {
        val m = new Attack(id.str, targetId.str)
        publish(m)
      }
      def said(id: Identity, what: String) = changeLog {
        val m = new thrift.message.Say(id.str, what)
        publish(m)
      }
      def shout(id: Identity, what: String) = changeLog {
        val m = new thrift.message.Shout(id.str, what)
        publish(m)
      }
      def toldOwnId(id: Identity) = changeLog {
        val m = new thrift.message.YourId(id.str)
        getChannel(id).write(m)
      }
      def toldPosition(id: Identity, targetId: Identity, position: Position) = changeLog {
        val m = new thrift.message.Position(targetId.str, position.x, position.z)
        getChannel(id).write(m)
      }
      def tellThings(id: Identity, tt: List[Thing]) = changeLog {
        val m = new thrift.message.Things()
        m.ts = new java.util.ArrayList[thrift.message.Thing]()
        tt.foreach { t =>
        // Things other than the client are included
          if (t.id != id) {
            val p = new thrift.message.Position(t.id.str, t.position.x.toDouble, t.position.z.toDouble)
            m.ts.add(new thrift.message.Thing(t.id.str, p))
          }
        }
        getChannel(id).write(m)
      }
//      case rep: thrift.message.Position =>
      //              val targetId = StringIdentity(m.id)
      //              val p = world.get().things.find(_.id == targetId).get.position
      //              val rep: thrift.message.Position = new thrift.message.Position(targetId.str, p.x.toFloat, p.z.toFloat)
      //              handle.asWritable.write(FrameEncoder(data))
    }

    def receiveLetter(letter: ReceivedLetter[AnyRef]) {
      val id = letter.sender
      atomic { txn =>
        log.debug("Beginning a transaction")
        server send {
          import thrift.message._
          log.debug("Receive & Replay")
          (_: WorldServer).receive(letter).replay()
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
        sender ! server.get().world
      case letter: ReceivedLetter[_] =>
        setChannel(letter.sender, ActorChannel(sender))
        receiveLetter(letter.asInstanceOf[ReceivedLetter[AnyRef]])
      case unexpected =>
        log.debug("Unexpected message: " + unexpected)
    }
  })

  lazy val server = createServer(1234)
}

case class ReceivedLetter[A](sender: Identity, message: A)
case class SendingLetter[A](recipient: Identity, message: A)
case class Change(id: Identity, message: AnyRef)
trait ChangeLog {
  def replay(): Unit
}
object ChangeLog {
  def apply(block: => Unit) = new ChangeLog {
    def replay = block
  }
}

case class WorldServer(world: InMemoryWorld) {

  val logger = LoggerFactory.getLogger(classOf[WorldServer])

  def receive(letter: ReceivedLetter[AnyRef]): WorldServer = {
    val sender = letter.sender
    val message = letter.message
    logger.debug("Received: " + letter)
    copy(
      message match {
        case m: thrift.message.Join =>
          world.join(new LivingPlayer(sender, 10f, Position(0f, 0f)))
        case m: thrift.message.Leave =>
          world.leave(new LivingPlayer(sender, 0f, Position(0f, 0f)))
        case m: thrift.message.MoveTo =>
          world.tryMoveTo(new LivingPlayer(sender, 10f, Position(0f, 0f)), Position(m.x.toFloat, m.z.toFloat))._1
        case m: thrift.message.Attack =>
          world.tryAttack(sender, StringIdentity(m.targetId))._1
        case m: thrift.message.Say =>
          world.trySay(sender, m.text)
        case m: thrift.message.Shout =>
          world.tryShout(sender, m.text)
        case m: thrift.message.MyId =>
          world.myId(sender)
        case m: thrift.message.GetPosition =>
          world.getPosition(sender, StringIdentity(m.id))
        case m: thrift.message.FindAllThings =>
          world.findAllThings(sender)
      }
    )
  }

  def replay(): WorldServer = copy(world = world.replay())
}
