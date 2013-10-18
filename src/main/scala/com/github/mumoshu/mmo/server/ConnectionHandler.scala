package com.github.mumoshu.mmo.server

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.io.{PipelineFactory, PipelineContext, Tcp}
import akka.io.Tcp.{PeerClosed, Write, Received}
import com.github.mumoshu.mmo.protocol.{LengthFieldFrame, HintFieldFrame, ThriftMessageFrame}
import akka.util.ByteString
import org.apache.thrift.TBase
import scala.util.{Failure, Success}
import com.github.mumoshu.mmo.models.world.world.StringIdentity
import com.github.mumoshu.mmo.server.tcpip.ActorChannel
import akka.event.LoggingReceive

/**
 * Above  --Command--> Below(=BelowCmdHandler)
 * Above(=AboveEventHandler) <-- Event --  Below
 */
trait PipelineInjectorBetweenServerAndClient {

  val aboveEventHandler: ActorRef
  val belowCmdHandler: ActorRef

  val stage = new ThriftMessageFrame >>
    new HintFieldFrame >>
    new LengthFieldFrame(10000)

  val ctx = new PipelineContext {
  }

  val pipeline = PipelineFactory.buildWithSinkFunctions(
    ctx,
    stage
  )(
    // Sends decoded commands to below
    belowCmdHandler ! _,
    // Sends encoded events to above
    aboveEventHandler ! _
  )

}

class ConnectionHandler(connection: ActorRef, world: ActorRef)
    extends Actor
    with ActorLogging
    with PipelineInjectorBetweenServerAndClient {

  val uuid = java.util.UUID.randomUUID().toString
  val identity = new StringIdentity(uuid)
  val aboveEventHandler = self // receives Try[AnyRef]
  val belowCmdHandler = self // receives Try[ByteString]

//  connection ! Write(ByteString("You are connected"))

  import Tcp._

  def receive = LoggingReceive {
    case Init =>
      val welcome = Welcome(identity, ActorChannel(self))
      log.debug("Sending Welcome: " + welcome)
      world ! welcome
    // Receives an above command and generates a below command
    case cmd: WorldCommand =>
      if (cmd.recipient != identity) {
        throw new RuntimeException(
          s"[BUG] connection handler for the id ${identity} received a command for the id ${cmd.recipient}!"
        )
      }

      // It will be transformed to Try[ByteString] and sent to below
      pipeline.injectCommand(cmd.message)
    // Receives a below command and send it via the connection
    case Success(data: ByteString) =>
      connection ! Write(data)
    // Receives a below event and generates an above event
    case Received(data) =>
      log.info(s"Received: ${data}")
      // It will be transformed to Try[AnyRef] and sent to above
      pipeline.injectEvent(data)
    // Receives a generated above event and sent it to the world
    case Success(msg: AnyRef) =>
      world ! WorldEvent(identity, msg)

    case Failure(e: Exception) =>
      log.error("Failure detected\n" + akka.event.Logging.stackTraceFor(e))

    // The connection has been closed by the remote endpoint
    case PeerClosed =>
      log.debug("PeerClosed")
      context stop self
  }
}
