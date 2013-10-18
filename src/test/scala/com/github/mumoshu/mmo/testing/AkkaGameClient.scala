package com.github.mumoshu.mmo.testing

import akka.actor.{ActorRef, IO, ActorLogging, Actor}
import akka.agent.Agent
import akka.actor.IO.Iteratee
import akka.pattern._
import bot._
import com.github.mumoshu.mmo.thrift
import com.github.mumoshu.mmo.models.world.world.{Identity, StringIdentity, Position}
import com.github.mumoshu.mmo.server.{Welcome, WorldCommand, WorldEvent, TCPIPServer}
import scala.concurrent.ExecutionContext
import akka.event.LoggingReceive
import com.github.mumoshu.mmo.server.tcpip.ActorChannel

// Stateful
class AkkaGameClient(id: Identity, server: ActorRef, observer: GameClientObserver)(implicit val executionContext: ExecutionContext) extends Actor with ActorLogging {

  log.debug("Connecting to an server actor: " + server.path)

  var oldSender: Option[ActorRef] = None
  implicit val system = context.system
  val waitingPosition = Agent[Option[ActorRef]](None)
  val waitingId = Agent[Option[ActorRef]](None)
  val waitingThings = Agent[Option[ActorRef]](None)

  import TCPIPServer.protocol._

  def processSingle(message: AnyRef) {
    message match {
      case t: thrift.message.Join =>
        observer.observe(t)
      case t: thrift.message.Leave =>
        observer.observe(t)
      case t: thrift.message.MoveTo =>
        observer.observe(t)
      case t: thrift.message.Attack =>
        observer.observe(t)
      case t: thrift.message.Say =>
        observer.observe(t)
      case t: thrift.message.Shout =>
        observer.observe(t)
      case t: thrift.message.YourId =>
        observer.observe(t)
        waitingId.get().foreach(_ ! StringIdentity(t.id))
        waitingId send { None }
      case t: thrift.message.Position =>
        observer.observe(t)
        waitingPosition.get().foreach(_ ! Position(t.x.toFloat, t.z.toFloat))
        waitingPosition send { None }
      case t: thrift.message.Things =>
        observer.observe(t)
        waitingThings.get().foreach(_ ! t)
        waitingThings send { None }
      case unexpected =>
        log.debug("Unexpected data received from the deserializer: " + unexpected)
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


  override def preStart() {
    super.preStart()
    server ! Welcome(id, ActorChannel(self))
  }

  def receive = LoggingReceive {
    case Send(thriftMessage: AnyRef) =>
      val bytes = serialize(thriftMessage)
      log.debug("Sending " + thriftMessage)
      server ! WorldEvent(id, thriftMessage)
//      sender ! true
    case FindAllThings =>
      waitingThings send {
        Some(sender)
      }
      val m = new thrift.message.FindAllThings()
      self ! Send(m)
    case GetPosition(id) =>
      waitingPosition send {
        Some(sender)
      }
      val m = new thrift.message.GetPosition(id.str)
      self ! Send(m)
    case AskForMyId =>
      waitingId send { id =>
        if (id.isDefined)
          throw new RuntimeException("waitingId is not empty!")
        Some(sender)
      }
      val m = new thrift.message.MyId()
      self ! Send(m)
    case WorldCommand(_, message) =>
      log.debug("Received: " + message)
      processSingle(message)
  }
}
