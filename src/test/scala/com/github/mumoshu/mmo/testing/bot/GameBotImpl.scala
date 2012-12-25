package com.github.mumoshu.mmo.testing.bot

import java.net.InetSocketAddress
import akka.actor.{Props, TypedActor, ActorSystem}
import akka.util
import com.github.mumoshu.mmo.server.TCPIPServer
import com.github.mumoshu.mmo.models.world.world.{StringIdentity, Identity, Position}
import concurrent.duration.Duration
import concurrent.Await
import com.github.mumoshu.mmo.testing.{GameClient, RecordingGameClientObserver, GameClientObserver}
import akka.pattern._
import concurrent.duration._

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
