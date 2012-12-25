package org.mmo

import akka.actor._

sealed trait State

// All the states are just singletons
case object Idle extends State
case object Started extends State
case object Died extends State

sealed trait Data

// Data can be an singleton or an instance of a case class
case object Uninitialized extends Data
case class Player(name: String, x: Float = 0f) extends Data
case class DiedPlayer(name: String) extends Data

// Events can be any objects

case class Join(name: String)
case object Start
case class Forward(dx: Float)
case object Die
case object Respawn
case object Leave

// - Create and pipe the WebSocket to a PlayerActor on connection
// - Stop and destroy the PlayerActor on disconnecting a WebSocket.
class PlayerActor extends Actor with FSM[State, Data] {

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Join(name), Uninitialized) =>
      stay using Player(name)
    case Event(Start, p: Player) =>
      goto(Started) using p
  }

  when(Started) {
    case Event(Forward(dx: Float), p: Player) =>
      goto(Started) using p.copy(x = p.x + dx)
    case Event(Die, p: Player) =>
      goto(Died) using DiedPlayer(p.name)
  }

  when(Died) {
    case Event(Respawn, DiedPlayer(name)) =>
      goto(Started) using Player(name)
  }

  whenUnhandled {
    case Event(Leave, p: Player) =>
      log.warning("Received: Leave " + p)
      stop
  }

  initialize

}
