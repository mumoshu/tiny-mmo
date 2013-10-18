package com.github.mumoshu.mmo.testing

import collection.mutable
import com.github.mumoshu.mmo.thrift.message._

// This is mutable
case class RecordingGameClientObserver(observedMessages: collection.mutable.MutableList[AnyRef] = mutable.MutableList.empty) extends GameClientObserver {
  def observe(t: Join) {
    observedMessages += t
  }

  def observe(t: Leave) {
    observedMessages += t
  }

  def observe(t: Attack) {
    observedMessages += t
  }

  def observe(t: MoveTo) {
    observedMessages += t
  }

  def observe(t: Say) {
    observedMessages += t
  }

  def observe(t: Shout) {
    observedMessages += t
  }

  def observe(t: Position) {
    observedMessages += t
  }

  def observe(t: YourId) {
    observedMessages += t
  }

  def observe(t: Things) {
    observedMessages += t
  }
}
