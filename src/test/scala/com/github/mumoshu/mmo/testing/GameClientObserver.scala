package com.github.mumoshu.mmo.testing

trait GameClientObserver {
  import serializers.thrift._
  def observe(t: serializers.thrift.Join)
  def observe(t: Leave)
  def observe(t: MoveTo)
  def observe(t: Attack)
  def observe(t: Say)
  def observe(t: Shout)
  def observe(t: serializers.thrift.Position)
  def observe(t: YourId)
  def observe(t: Things)
}
