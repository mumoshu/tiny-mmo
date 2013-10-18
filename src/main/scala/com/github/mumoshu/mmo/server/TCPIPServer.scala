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
import org.slf4j.LoggerFactory
import world.world.InMemoryWorld
import world.world.LivingPlayer
import world.world.Position
import com.github.mumoshu.mmo.models.Terrain
import scala.concurrent.ExecutionContext
import akka.io
import akka.io.Tcp
import akka.io.Tcp._
import com.github.mumoshu.mmo.models.world.world.InMemoryWorld
import com.github.mumoshu.mmo.models.Terrain
import com.github.mumoshu.mmo.models.world.world.StringIdentity

import ExecutionContext.Implicits._

/**
 * See http://stackoverflow.com/questions/12959709/send-a-tcp-ip-message-akka-actor
 * and http://doc.akka.io/docs/akka/snapshot/scala/io.html
 */

object TCPIPServer extends TCPIPServer()

// TODO Rename to AkkaWorldServer
class TCPIPServer(implicit val executionContext: ExecutionContext) {

  val log = LoggerFactory.getLogger(classOf[TCPIPServer])

  implicit val sys = ActorSystem("tcpIpServerSystem")

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

  lazy val worldActor = sys.actorOf(Props(new WorldActor), name = "world")

  def createServer(port: Int = 1234) = sys.actorOf(
    Props(new TcpIpServerActor(
      port = port,
      worldActor = worldActor
    )),
    name = "tcpIpServerActor"
  )

  lazy val server = createServer(1234)
}

class TcpIpServerActor(port: Int, worldActor: ActorRef)(implicit sys: ActorSystem, ctx: ExecutionContext) extends Actor with ActorLogging {

  implicit val any2ByteString = DefaultByteStringWriter

  log.debug("Starting server")

  val address = new InetSocketAddress(port)
  val socket = io.IO(Tcp) ! Bind(self, address)

  log.debug("Now listening on " + address)

  import Tcp._

  def receive = {
    case b @ Bound(localAddress) =>
      log.debug("Bound: localAddress=" + localAddress)

    case CommandFailed(_: Bind) =>
      log.debug("CommandFailed")
      context stop self

    case c @ Connected(remote, local) =>
      val connection = sender

      val name = s"connectionHandler:remote=${remote},local=${local}".replaceAll("[^\\d]", "-")
      val handler = context.actorOf(Props(
        new ConnectionHandler(connection, worldActor)
      ), name = name)

    log.debug("Connection: " + connection)

      handler ! Init
      connection ! Register(handler)

      log.debug("Connected: remote=" + remote + ", local=" + local)

    case "STOP" =>
      log.debug("Closing the server socket")
      context stop self
    case "WORLD" =>
      worldActor ! RespondToOther("WORLD", sender)
    case unexpected =>
      log.debug("Unexpected message: " + unexpected)
  }
}

case class RespondToOther(request: AnyRef, other: ActorRef)

class WorldActor extends Actor with ActorLogging {
  lazy val channels = new Channels
  lazy val worldChangeHandler = BatchingWorldChangeHandler(channels)
  lazy val currentWorld = Agent(new EventedWorld(
    new InMemoryWorld(List.empty, Terrain(Array(Array(Tile.Ground, Tile.Ground))), List.empty, worldChangeHandler)
  ))

  def handleEvent(worldEvent: WorldEvent) {
    atomic { txn =>
      log.debug("Beginning a transaction")
      currentWorld send { world =>
        log.debug("Receive & Replay")
        world.appliedEvent(worldEvent).sendCommands()
      }
    }
  }

  def receive = {
    case Welcome(identity, channel) =>
      channels.accept(identity, channel)
    case worldEvent: WorldEvent =>
      handleEvent(worldEvent)
    case RespondToOther("WORLD", dest) =>
      dest ! currentWorld.get().world
  }
}

case class Welcome(identity: Identity, connectionHandler: Channel)

/**
 * The WorldEvents are messages sent from entities in the world to the World.
 * The world changes its state by committing the events reported by the entities.
 * @param sender who reported the event
 * @param message what happened by the event
 */
case class WorldEvent(sender: Identity, message: AnyRef)

/**
 * The WorldCommands are messages sent from the World to entities in the World.
 * All the entities involved in an event syncs their state via these commands.
 * @param recipient who sees the event
 * @param message what happened by the event
 */
case class WorldCommand(recipient: Identity, message: AnyRef)

case object Init
