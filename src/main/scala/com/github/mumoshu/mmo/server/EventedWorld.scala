package com.github.mumoshu.mmo.server

import com.github.mumoshu.mmo.models.world.world.{StringIdentity, Position, LivingPlayer, InMemoryWorld}
import org.slf4j.LoggerFactory
import com.github.mumoshu.mmo.thrift

case class EventedWorld(world: InMemoryWorld) {

  val logger = LoggerFactory.getLogger(classOf[EventedWorld])

  def appliedEvent(worldEvent: WorldEvent): EventedWorld = {
    val sender = worldEvent.sender
    val message = worldEvent.message
    logger.debug("WorldEvent: " + worldEvent)
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

  def sendCommands(): EventedWorld = copy(world = world.replayChanges())
}
