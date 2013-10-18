package com.github.mumoshu.mmo.testing

import com.github.mumoshu.mmo.thrift.message._

trait GameClientObserver {
  def observe(t: Join)
  def observe(t: Leave)
  def observe(t: MoveTo)
  def observe(t: Attack)
  def observe(t: Say)
  def observe(t: Shout)
  def observe(t: Position)
  def observe(t: YourId)
  def observe(t: Things)
  def observe(t: Presentation)
}
