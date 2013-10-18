package com.github.mumoshu.mmo.server

import com.github.mumoshu.mmo.models.WorldChangeHandler
import com.github.mumoshu.mmo.models.world.world._
import com.github.mumoshu.mmo.thrift.message._
import com.github.mumoshu.mmo.thrift
import com.github.mumoshu.mmo.models.world.world.Position
import com.github.mumoshu.mmo.models.world.world.Thing
import scala.collection.JavaConversions._

case class BatchingWorldChangeHandler(channels: Channels, worldChanges: List[WorldChange] = List.empty) extends WorldChangeHandler {
  def pend(block: => Unit): WorldChangeHandler = copy(worldChanges = worldChanges :+ WorldChange(block))

  def handleAllChanges(): BatchingWorldChangeHandler = {
    worldChanges.foreach(_.replay())
    copy(worldChanges = List.empty)
  }

  def joined(id: Identity, name: String) = pend {
    val m = new Join()
    m.name = name
    channels.publish(m)
  }

  def left(id: Identity) = pend {
    val m = new Leave()
    channels.remove(id)
    channels.publish(m)
  }

  def movedTo(id: Identity, position: Position) = pend {
    val m = new MoveTo(id.str, position.x, position.z)
    publish(m)
  }
  def attacked(id: Identity, targetId: Identity) = pend {
    val m = new Attack(id.str, targetId.str)
    publish(m)
  }
  def said(id: Identity, what: String) = pend {
    val m = new thrift.message.Say(id.str, what)
    publish(m)
  }
  def shout(id: Identity, what: String) = pend {
    val m = new thrift.message.Shout(id.str, what)
    publish(m)
  }
  def toldOwnId(id: Identity) = pend {
    val m = new thrift.message.YourId(id.str)
    channels.writeTo(id, m)
  }
  def toldPosition(id: Identity, targetId: Identity, position: Position) = pend {
    val m = new thrift.message.Position(targetId.str, position.x, position.z)
    channels.writeTo(id, m)
  }

  def tellThings(id: Identity, tt: List[Thing]) = pend {
    val m = new thrift.message.Things()
    m.ts = new java.util.ArrayList[thrift.message.Thing]()
    tt.foreach { t =>
    // Things other than the client are included
      if (t.id != id) {
        val p = new thrift.message.Position(t.id.str, t.position.x.toDouble, t.position.z.toDouble)
        m.ts.add(new thrift.message.Thing(t.id.str, p))
      }
    }
    channels.writeTo(id, m)
  }

  def presentationCreated(p: Presentation) = pend {
    val m = p.deepCopy()
    m.setUrl(m.getUrl.replace("server", "client"))
    channels.all.foreach { case (id, ch) =>
      id match {
        case StringIdentity(strId) if strId == p.ownerId =>
          ch.write(WorldCommand(id, p))
        case _ =>
          ch.write(WorldCommand(id, m))
      }
    }
  }
  //      case rep: thrift.message.Position =>
  //              val targetId = StringIdentity(m.id)
  //              val p = world.get().things.find(_.id == targetId).get.position
  //              val rep: thrift.message.Position = new thrift.message.Position(targetId.str, p.x.toFloat, p.z.toFloat)
  //              handle.asWritable.write(FrameEncoder(data))

  def publish(m: AnyRef) = {
    channels.publish(m)
  }

}
