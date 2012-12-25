package com.github.mumoshu.mmo.testing

import collection.mutable
import com.github.mumoshu.mmo.thrift.message._

// This is mutable
case class RecordingGameClientObserver(observed: collection.mutable.MutableList[AnyRef] = mutable.MutableList.empty) extends GameClientObserver {
  def observe(t: Join) {
    observed += t
  }

  def observe(t: Leave) {
    observed += t
  }

  def observe(t: Attack) {
    observed += t
  }

  def observe(t: MoveTo) {
    observed += t
  }

  def observe(t: Say) {
    observed += t
  }

  def observe(t: Shout) {
    observed += t
  }

  def observe(t: Position) {
    observed += t
  }

  def observe(t: YourId) {
    observed += t
  }

  def observe(t: Things) {
    observed += t
  }
}
