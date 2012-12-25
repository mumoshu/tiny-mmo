package com.github.mumoshu.mmo.testing.bot

import com.github.mumoshu.mmo.models.world.world.{Identity, Position}
import com.github.mumoshu.mmo.testing.GameClientObserver

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
