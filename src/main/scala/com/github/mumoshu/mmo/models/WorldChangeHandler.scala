package com.github.mumoshu.mmo.models

import world.world.{Thing, Position, Identity}

trait WorldChangeHandler {
  def pend(block: => Unit): WorldChangeHandler
  def handleAllChanges(): WorldChangeHandler
  def joined(id: Identity, name: String): WorldChangeHandler
  def left(id: Identity): WorldChangeHandler
  def movedTo(id: Identity, position: Position): WorldChangeHandler
  def attacked(id: Identity, targetId: Identity): WorldChangeHandler
  def said(id: Identity, what: String): WorldChangeHandler
  def shout(id: Identity, what: String): WorldChangeHandler
  def toldOwnId(id: Identity): WorldChangeHandler
  def toldPosition(id: Identity, targetId: Identity, position: Position): WorldChangeHandler
  def tellThings(id: Identity, tt: List[Thing]): WorldChangeHandler
}

