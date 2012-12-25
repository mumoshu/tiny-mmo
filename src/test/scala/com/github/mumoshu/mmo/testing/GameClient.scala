package com.github.mumoshu.mmo.testing

import akka.actor.{ActorRef, IO, ActorLogging, Actor}
import akka.agent.Agent
import akka.actor.IO.Iteratee
import akka.actor.IOManager
import akka.pattern._
import akka.util.ByteString
import com.github.mumoshu.mmo.models.world.world.{Position, StringIdentity}
import com.github.mumoshu.mmo.server.TCPIPServer
import com.github.mumoshu.mmo.server.TCPIPServer.FrameEncoder
import java.net.InetSocketAddress

import bot._

// Stateful
class GameClient(address: InetSocketAddress, observer: GameClientObserver) extends Actor with ActorLogging {

  log.debug("Connecting to " + address)

  // Using this socket assuming that exactly one bot connects the server with GameClient
  val socket = IOManager(context.system).connect(address)
  val state = IO.IterateeRef.Map.async[IO.Handle]()(context.dispatcher)
  // Using those vars assuming that exactly one bot connects the server with GameClient
  var sock: Option[IO.SocketHandle] = None
  var oldSender: Option[ActorRef] = None
  implicit val system = context.system
  val waitingPosition = Agent[Option[ActorRef]](None)
  val waitingId = Agent[Option[ActorRef]](None)
  val waitingThings = Agent[Option[ActorRef]](None)

  log.debug("Starting with socket " + socket.uuid)

  import TCPIPServer.protocol._

  def processSingle(socket: IO.SocketHandle, bytes: ByteString) {
    deserialize(bytes) match {
      case t: serializers.thrift.Join =>
        observer.observe(t)
      case t: serializers.thrift.Leave =>
        observer.observe(t)
      case t: serializers.thrift.MoveTo =>
        observer.observe(t)
      case t: serializers.thrift.Attack =>
        observer.observe(t)
      case t: serializers.thrift.Say =>
        observer.observe(t)
      case t: serializers.thrift.Shout =>
        observer.observe(t)
      case t: serializers.thrift.YourId =>
        observer.observe(t)
        waitingId.get().foreach(_ ! StringIdentity(t.id))
        waitingId send { None }
      case t: serializers.thrift.Position =>
        observer.observe(t)
        waitingPosition.get().foreach(_ ! Position(t.x.toFloat, t.z.toFloat))
        waitingPosition send { None }
      case t: serializers.thrift.Things =>
        observer.observe(t)
        waitingThings.get().foreach(_ ! t)
        waitingThings send { None }
      case unexpected =>
        log.debug("Unexpected data received from the deserializer: " + unexpected)
        log.debug("ByteString#toString=" + bytes.utf8String)
        log.debug("Maybe the server is sending new messages unknown to me")
    }
    // case YourIdentity(identity) => this.identity = identity
  }

  def processData(socket: IO.SocketHandle): Iteratee[Unit] = {
    IO repeat {
      for {
        bytes <- TCPIPServer.FrameDecoder
      } yield {
        processSingle(socket, bytes)
      }
    }
  }

  def receive = {
    case IO.Connected(socket, address) =>
      log.debug("Connected to a server (handle.uuid=" + socket.uuid + ")")
      sock = Some(socket)
      state(socket) flatMap (_ => processData(socket))
    case IO.Read(socket, bytes) =>
      log.debug("Read " + bytes.toString)
      state(socket)(IO Chunk bytes)
    case Send(thriftMessage: AnyRef) =>
      val bytes = serialize(thriftMessage)
      log.debug("Sending " + thriftMessage)
      oldSender = Some(sender)
      sock.foreach { s =>
        log.debug("To server with the handle: " + s.uuid)
        s.write(FrameEncoder(bytes.compact))
//        s.close()
        log.debug(bytes.length + " bytes sent")
      }
      oldSender.foreach { s => s ! true }
    case FindAllThings =>
      waitingThings send {
        Some(sender)
      }
      sock.foreach { s =>
        val m = new serializers.thrift.FindAllThings()
        self ! Send(m)
      }
    case GetPosition(id) =>
      waitingPosition send {
        Some(sender)
      }
      sock.foreach { s =>
        val m = new serializers.thrift.GetPosition(id.str)
        self ! Send(m)
      }
    case AskForMyId =>
      waitingId send { id =>
        if (id.isDefined)
          throw new RuntimeException("waitingId is not empty!")
        Some(sender)
      }
      sock.foreach { s =>
        val m = new serializers.thrift.MyId()
        self ! Send(m)
      }
  }
}
