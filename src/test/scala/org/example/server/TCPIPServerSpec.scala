package org.example.server

import org.specs2.mutable._
import java.net.InetSocketAddress
import akka.actor._
import akka.pattern._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.{ByteString, Timeout}
import org.example.models.world.world._
import akka.util
import serializers.thrift._
import scala.Some
import collection.mutable
import akka.agent.Agent
import scala.concurrent.stm._
import org.example.server.RecordingGameClientObserver
import scala.Some
import org.example.server.GameBotImpl
import org.example.server.GetPosition
import org.example.models.world.world.Position
import org.example.server.Send
import serializers.thrift.Shout
import serializers.thrift.Say
import serializers.thrift
import org.example.server.TCPIPServer.FrameEncoder
import akka.actor.IO.Iteratee

case class GetPosition(id: Identity)
case object AskForMyId
case object FindAllThings

trait GameClientObserver {
  import serializers.thrift._
  def observe(t: serializers.thrift.Join)
  def observe(t: Leave)
  def observe(t: MoveTo)
  def observe(t: Attack)
  def observe(t: Say)
  def observe(t: Shout)
  def observe(t: serializers.thrift.Position)
  def observe(t: YourId)
  def observe(t: Things)
}

// This is mutable
case class RecordingGameClientObserver(observed: collection.mutable.MutableList[AnyRef] = mutable.MutableList.empty) extends GameClientObserver {
  def observe(t: serializers.thrift.Join) {
    observed += t
  }

  def observe(t: Leave) {
    observed += t
  }

  def observe(t: Attack) {
    observed += t
  }

  def observe(t: MoveTo) {
    observed += t
  }

  def observe(t: Say) {
    observed += t
  }

  def observe(t: Shout) {
    observed += t
  }

  def observe(t: thrift.Position) {
    observed += t
  }

  def observe(t: YourId) {
    observed += t
  }

  def observe(t: Things) {
    observed += t
  }
}

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

trait GameBot {
  def observer: GameClientObserver
  def join()
  def moveTo(p: Position)
  def leave()
  def attack(id: Identity)
  def say(what: String)
  def shout(what: String)
  def nearby: Option[List[Identity]]
  def getPos(id: Identity): Option[Position]
  def selfId: Option[Identity]
  def nearest: Option[Identity]
  def follow(id: Identity)
}

case class Send(message: AnyRef)

// Stateful
case class GameBotImpl(serverAddress: InetSocketAddress, playerName: String = "mumoshu", system: ActorSystem, observer: GameClientObserver = RecordingGameClientObserver()) extends GameBot { // with GameClientObserver

  import TypedActor.dispatcher

  val client = system.actorOf(Props(new GameClient(serverAddress, observer)))

  implicit val timeout = util.Timeout(5 seconds)

  import TCPIPServer.protocol._

  def appear(p: Position) {
    client ! Send(new serializers.thrift.Appear(selfId.get.str, p.x.toDouble, p.z.toDouble))
  }

  def disappear() {
    client ! Send(new serializers.thrift.Disappear(selfId.get.str))
  }

  def join() {
    client ! Send(new serializers.thrift.Join(playerName))
  }

  def moveTo(p: Position) {
    client ! Send(new serializers.thrift.MoveTo(selfId.get.str, p.x, p.z))
  }

  def leave() {
    client ! Send(new serializers.thrift.Leave())
  }

  def attack(id: Identity) {
    client ! Send(new serializers.thrift.Attack(selfId.get.str, id.str))
  }

  def say(what: String) {
    client ! Send(new serializers.thrift.Say(selfId.get.str, what))
  }

  def shout(what: String) {
    client ! Send(new serializers.thrift.Say(selfId.get.str, what))
  }

  val duration = Duration("3 seconds")

  import collection.JavaConverters._

  // val sight = "10 meters"
  def nearby: Option[List[Identity]] = Some(
    Await.result(
      (client ? FindAllThings)
        .mapTo[serializers.thrift.Things]
        .map(_.ts.asScala.toList.map(_.id).map(StringIdentity)),
      duration)
  )
  def getPos(id: Identity): Option[Position] = Some(Await.result((client ? GetPosition(id)).mapTo[Position], duration))
  lazy val selfId: Option[Identity] = Some(Await.result((client ? AskForMyId).mapTo[Identity], duration))

  def nearest: Option[Identity] = Some(nearby.get.minBy(id => getPos(selfId.get).get.distance(getPos(id).get)))

  def follow(id: Identity) {
    val start = getPos(selfId.get).get
    val end = getPos(id).get
    val p = start.lerp(end, 0.1f)
    moveTo(p)
  }

}

object Tester {

  def test = {
    val system = ActorSystem("client")
    val server = TCPIPServer.server
    Thread.sleep(1100)
    val client = system.actorOf(Props[GameClient])
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

//      val result = Tester.test
//      result must be some
//
      Thread.sleep(1100)

//      TCPIPServer.world.things must be size(1)
    }

    "integration" in new After {

      val system = ActorSystem("integration")
      val port = 1235
      val serverAddress = new InetSocketAddress("localhost", port)
      def makeBot(name: String): GameBot = {
        TypedActor(system).typedActorOf(
          props = TypedProps(classOf[GameBot], new GameBotImpl(serverAddress, name, system)),
          name = "gameBot-" + name
        )
      }

      implicit val timeout = util.Timeout(FiniteDuration(5, concurrent.duration.SECONDS))

      val server = TCPIPServer.createServer(port)

      // You need this to wait for the server to start listening
      val world0 = Await.result((server ? "WORLD").mapTo[InMemoryWorld], Duration("5 seconds"))

      val numberOfBots = 3
      val bots = (1 to numberOfBots).map(_.toString).map(makeBot)

      Thread.sleep(3000)

      val world1 = Await.result((server ? "WORLD").mapTo[InMemoryWorld], Duration("5 seconds"))

      world1.things must be size(0)

      bots.foreach { b =>
        b.join()
        Thread.sleep(500)
      }

      Thread.sleep(2000)

      val world2 = Await.result((server ? "WORLD").mapTo[InMemoryWorld], Duration("5 seconds"))
      world2.things must be size(3)

      bots(0).leave()

      Thread.sleep(2000)

      val world3 = Await.result((server ? "WORLD").mapTo[InMemoryWorld], Duration("5 seconds"))
      world3.things must be size(2)

      bots(0).observer.asInstanceOf[RecordingGameClientObserver].observed must be size(3) // bots(0) joins and then bots(1) joins, then bots(2) joins
      bots(1).observer.asInstanceOf[RecordingGameClientObserver].observed must be size(3) // bots(1) joins and then bots(2) joins, then bots(0) leaves
      bots(2).observer.asInstanceOf[RecordingGameClientObserver].observed must be size(2) // bots(2) joins and then bots(0) leaves

      val selfId: Identity = bots(1).selfId.get
      println("bots(1).selfId: " + selfId)
      bots(1).nearby.get must be size(1)
      val nearby = bots(1).nearby.get
      println("nearby: " + nearby)
      val nearest = bots(1).nearest.get
      nearby must be size (1)
      nearby(0) must be equalTo (nearest)
      val targetId = nearest
      println("Target pos: " + bots(1).getPos(targetId))
      bots(1).say("Hi!")
      bots(1).shout("Hi!!")
      bots(1).moveTo(Position(1f, 2f))
      Thread.sleep(500)
      bots(1).getPos(selfId).get must be equalTo (Position(1f, 2f))
      bots(1).follow(targetId)
      bots(1).getPos(selfId) must not be equalTo (Position(1f, 2f))
      bots(1).attack(targetId)

      println(bots(1).observer.asInstanceOf[RecordingGameClientObserver].observed)
      println(bots(2).observer.asInstanceOf[RecordingGameClientObserver].observed)

      def after {
        server ! "STOP"
      }

    }
  }
}
