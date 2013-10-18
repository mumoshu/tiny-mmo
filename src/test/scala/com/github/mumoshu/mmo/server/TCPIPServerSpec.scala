package com.github.mumoshu.mmo.server

import org.specs2.mutable._
import java.net.InetSocketAddress
import akka.actor._
import akka.pattern._
import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration._
import akka.util.{ByteString, Timeout}
import com.github.mumoshu.mmo.models.world.world._
import akka.util
import com.github.mumoshu.mmo.testing._
import bot.{GameBotImpl, GameBot}
import com.github.mumoshu.mmo.models.world.world.Position
import com.github.mumoshu.mmo.testing.RecordingGameClientObserver
import com.github.mumoshu.mmo.thrift
import scala.concurrent.ExecutionContext.Implicits.global
import org.specs2.execute.Result
import org.specs2.specification.Context
import com.github.mumoshu.mmo.thrift.message.{Quaternion, Vector3, Presentation}

object Tester {

  def test = {
    val system = ActorSystem("client")
    val server = TCPIPServer.server
    Thread.sleep(1100)
    val client = system.actorOf(Props[TcpIpGameClient])
    Thread.sleep(1100)
    implicit val timeout = Timeout(5 seconds)
    val bytes = TCPIPServer.protocol.serialize(new thrift.message.Join("mumoshu"))
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

class TCPIPServerSpec extends Specification {

  sequential

  trait tcpIpServerContext extends Context with After {

    val system = ActorSystem("integration")
    def port: Int
    val worldActor = TCPIPServer.worldActor
    val tcpIpServerActor = TCPIPServer.createServer(port)
    val serverAddress = new InetSocketAddress("localhost", port)

    implicit val timeout = util.Timeout(FiniteDuration(5, concurrent.duration.SECONDS))

    def makeTcpIpBot(name: String): (GameBot, RecordingGameClientObserver) = {
      val observer = RecordingGameClientObserver()
      val client = system.actorOf(Props(new TcpIpGameClient(serverAddress, observer)), name = s"gamebot-${name}")
      (
        TypedActor(system).typedActorOf(
          props = TypedProps(classOf[GameBot], new GameBotImpl(client, name)),
          name = "gameBot-" + name
        ),
        observer
        )
    }

    def makeAkkaBot(name: String): (GameBot, RecordingGameClientObserver) = {
      val observer = RecordingGameClientObserver()
      val id = StringIdentity(java.util.UUID.randomUUID().toString)
      val client = system.actorOf(Props(
        new AkkaGameClient(id, worldActor, observer)(ExecutionContext.global)),
        name = s"akkaBot-${name}"
      )
      (
        TypedActor(system).typedActorOf(
          props = TypedProps(classOf[GameBot], new GameBotImpl(client, name)),
          name = "akkaGameBot-" + name
        ),
        observer
        )
    }

    def captureWorld = {
      Await.result((tcpIpServerActor ? "WORLD").mapTo[InMemoryWorld], Duration("5 seconds"))
    }

    def sleep(millis: Long) {
      Thread.sleep(millis)
    }

    def after {
      println("Cleaning up the test environment...")
      tcpIpServerActor ! "STOP"
    }

  }

  "TCPIPServer" should {

    "mediate messages between clients" in new tcpIpServerContext {

      def port = 1234

      // You need this to wait for the server to start listening
      val world0 = captureWorld

      val numberOfBots = 3
      val botConfigs = (1 to numberOfBots - 1).map(_.toString).map(makeTcpIpBot) ++ List(numberOfBots.toString).map(makeAkkaBot)
      val bots = botConfigs.map(_._1)
      val observers = botConfigs.map(_._2)

      Thread.sleep(3000)

      val world1 = captureWorld

      world1.things must be size(0)

      bots.foreach { b =>
        b.join()
        Thread.sleep(500)
      }

      Thread.sleep(2000)

      val world2 = captureWorld
      world2.things must be size(3)

      bots(0).leave()

      Thread.sleep(2000)

      val world3 = captureWorld
      world3.things must be size(2)

      observers(0).observedMessages must be size(3) // bots(0) joins and then bots(1) joins, then bots(2) joins
      observers(1).observedMessages must be size(4) // bots(1) joins and then bots(2) joins, then bots(0) leaves
      observers(2).observedMessages must be size(4) // bots(2) joins and then bots(0) leaves

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

      println(observers(1).observedMessages)
      println(observers(2).observedMessages)

    }

    "provide the presentation sharing functionality" in new tcpIpServerContext {

      def port = 1235

      val numBots = 2
      val botsAndObservers = (1 to numBots).map(_.toString).map(makeTcpIpBot)
      val bots = botsAndObservers.map(_._1)
      val observers = botsAndObservers.map(_._2)

      sleep(1000)

      bots.foreach(_.join())

      sleep(1000)

      val id = bots(0).selfId.get.str

      def randomVector3 = {
        import scala.math.random
        val v = new Vector3
        v.setX(random)
        v.setY(random)
        v.setZ(random)
        v
      }

      def randomQuaternion = {
        import scala.math.random
        val q = new Quaternion
        q.setX(random)
        q.setY(random)
        q.setZ(random)
        q.setW(random)
        q
      }

      val presentationId = java.util.UUID.randomUUID().toString
      val position = randomVector3
      val rotation = randomQuaternion
      val zoom = randomVector3
      val p = new Presentation
      p.setId(presentationId)
      p.setOwnerId(id)
      p.setPosition(position)
      p.setRotation(rotation)
      p.setZoom(zoom)
      p.setUrl("http://localhost/server.html")
      bots(0).startPresentation(p)

      sleep(500)

      val observedByOwner = observers(0).observedMessages.find(_.isInstanceOf[Presentation]).get.asInstanceOf[Presentation]
      val observedByMember = observers(1).observedMessages.find(_.isInstanceOf[Presentation]).get.asInstanceOf[Presentation]

      observedByOwner.getUrl must beEqualTo ("http://localhost/server.html")
      observedByMember.getUrl must beEqualTo ("http://localhost/client.html")
    }
  }
}
