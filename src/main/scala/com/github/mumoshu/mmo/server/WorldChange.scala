package com.github.mumoshu.mmo.server

trait WorldChange {
  def replay(): Unit
}

object WorldChange {
  def apply(block: => Unit) = new WorldChange {
    def replay = block
  }
}

