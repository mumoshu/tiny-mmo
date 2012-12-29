package com.github.mumoshu.mmo.testing.bot

import java.net.InetSocketAddress
import akka.actor.{ActorRef, Props, TypedActor, ActorSystem}
import akka.util
import com.github.mumoshu.mmo.server.TCPIPServer
import com.github.mumoshu.mmo.models.world.world.{StringIdentity, Identity, Position}
import com.github.mumoshu.mmo.thrift
import concurrent.duration.Duration
import concurrent.Await
import com.github.mumoshu.mmo.testing.{GameClient, RecordingGameClientObserver, GameClientObserver}
import akka.pattern._
import concurrent.duration._

// Stateful
case class GameBotImpl(client: ActorRef, playerName: String = "mumoshu") extends GameBot { // with GameClientObserver

  import TypedActor.dispatcher

  implicit val timeout = util.Timeout(5 seconds)

  import TCPIPServer.protocol._

  def appear(p: Position) {
    client ! Send(new thrift.message.Appear(selfId.get.str, p.x.toDouble, p.z.toDouble))
  }

  def disappear() {
    client ! Send(new thrift.message.Disappear(selfId.get.str))
  }

  def join() {
    client ! Send(new thrift.message.Join(playerName))
  }

  def moveTo(p: Position) {
    client ! Send(new thrift.message.MoveTo(selfId.get.str, p.x, p.z))
  }

  def leave() {
    client ! Send(new thrift.message.Leave())
  }

  def attack(id: Identity) {
    client ! Send(new thrift.message.Attack(selfId.get.str, id.str))
  }

  def say(what: String) {
    client ! Send(new thrift.message.Say(selfId.get.str, what))
  }

  def shout(what: String) {
    client ! Send(new thrift.message.Say(selfId.get.str, what))
  }

  val duration = Duration("3 seconds")

  import collection.JavaConverters._

  // val sight = "10 meters"
  def nearby: Option[List[Identity]] = Some(
    Await.result(
      (client ? FindAllThings)
        .mapTo[thrift.message.Things]
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
