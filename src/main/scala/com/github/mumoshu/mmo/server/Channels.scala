package com.github.mumoshu.mmo.server

import com.github.mumoshu.mmo.models.world.world.Identity
import com.github.mumoshu.mmo.server.tcpip.Channel
import scala.concurrent.stm._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._

class Channels {
  private val channels = new ConcurrentHashMap[Identity, Channel]

  def set(userId: Identity, channel: Channel) {
    channels.put(userId, channel)
  }

  def accept(userId: Identity, channel: Channel) {
    println(s"ACCEPTED: ${userId}, ${channel}")
    set(userId, channel)
  }

  def remove(userId: Identity): Channels = {
    channels.remove(userId)
    this
  }

  private def get(userId: Identity): Channel = {
    channels.get(userId)
  }

  def writeTo(dest: Identity, msg: AnyRef) = {
    get(dest).write(WorldCommand(dest, msg))
  }

  def publish(msg: AnyRef): Channels = {
    println("PUBLISHING START")
    channels.foreach { case (identity, channel) =>
      val cmd = WorldCommand(identity, msg)
      println(s"PUBLISHING: ${cmd}")
      channel.write(cmd)
    }
    this
  }

  def all = channels

}
