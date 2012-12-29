package com.github.mumoshu.mmo.models

import world.world.{Thing, Position, Identity}

trait ChangeLogger {
  def changeLog(block: => Unit): ChangeLogger
  def replay(): ChangeLogger
  def joined(id: Identity, name: String): ChangeLogger
  def left(id: Identity): ChangeLogger
  def movedTo(id: Identity, position: Position): ChangeLogger
  def attacked(id: Identity, targetId: Identity): ChangeLogger
  def said(id: Identity, what: String): ChangeLogger
  def shout(id: Identity, what: String): ChangeLogger
  def toldOwnId(id: Identity): ChangeLogger
  def toldPosition(id: Identity, targetId: Identity, position: Position): ChangeLogger
  def tellThings(id: Identity, tt: List[Thing]): ChangeLogger
}

