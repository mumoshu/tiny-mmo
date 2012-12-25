package com.github.mumoshu.mmo.experimental.model

import scala.reflect.runtime.universe._

class Identity(val underlying: Long) extends AnyVal

object Identity {

  case class BeingIdentity[A <: Being](id: Identity)

  def AvatarIdentity(underlying: Long) = BeingIdentity[Avatar](new Identity(underlying))
  def CreatureIdentity(underlying: Long) = BeingIdentity[Creature](new Identity(underlying))

}

trait Being {
  val id: Identity
}

// Beingが起こした行動
sealed trait Action
case class Say(what: String) extends Action
case class Shout(what: String) extends Action

case class Avatar(id: Identity) extends Being

case class State[A <: Being](being: A, actions: Seq[Action])

object Avatar {
  implicit def avatarToActor(avatar: Avatar) = new {
    def say(what: String) = avatar
    def shout(what: String) = avatar
  }
}

object AvatarToCreatureInteraction {
  implicit def a2b(avatar: Avatar) = new {

  }
}

case class Villager(id: Identity) extends Being {
  type Self = Villager
}
case class Creature(id: Identity) extends Being {
  type Self = Creature
}
case class Treasure(id: Identity) extends Being {
  type Self = Treasure
}

case class TypedBeing(tpe: Type, being: Being)

case class World(beings: List[TypedBeing] = List.empty) {
  def arise[A <: Being : TypeTag](being: A) = copy(beings = beings :+ TypedBeing(typeOf[A], being))
  def occur[A <: Being : TypeTag](id: Identity)(e: A => A) = copy(beings = beings.map {
    case tb if tb.tpe =:= typeOf[A] =>
      tb.copy(being = tb.being)
    case tb =>
      tb
  })
  //  def interact[A <: Being : TypeTag, B <: Being : TypeTag](id1: Identity, id2: Identity) =
  //    beings.filter(b => b.being.id == id1 && b.tpe =:= typeOf[A] || b.being.id == id2 && b.tpe =:= typeOf[B]) match {
  //      case List(a, b) =>
  //
  //    }
  def vanish[A <: Being : TypeTag](id: Identity) = copy(beings = beings.filter(tb => tb.tpe != typeOf[A] || tb.being.id != id ))
}

object Example {

  val avatar1Id = new Identity(1L)
  val creature1Id = new Identity(2L)

  World().arise(Avatar(avatar1Id)).arise(Creature(creature1Id)).occur[Avatar](avatar1Id)(_.say("hi")).vanish[Creature](creature1Id)

  /**

  // tcp server manages many clients

  val state = Agent(State)

  // on receive bytes from clients (push from clients to server)
  for {
    bytes <- iteratee
  } yield {
    val messages = unmarshal(bytes)

    messages foreach { m =>
      server ! m
    }
  }

  // on receive messages from server (push from server to clients)
  receive { m =>
    router.route(marshal(m))
  }

  // AI (creature or villager, treasure) is single

  // on receive messages from server
  val state = Agent(State)

  self ! Init

  receive { m =>
    state send (_.update(m))
  }

  // do something every second with intelligence
  interval(1 second) {
    val messages = update()

    messages foreach { m =>
      server ! m
    }
  }

  // Akka server

  atomic { txn =>
    world send {
      messages.foldLeft(world) { (world, message) =>
        val (world, newMessages) = change(world, message)
        newMessages.foreach { m =>
          // Send events to clients or AIs in concern (in sight, related, etc.)
          akkaRouter.route(m)
        }
      }
    )
  }

  def change(world: World, m: Message): (World, Seq[Message]) =
    import com.github.mumoshu.mmo.thrift.message._
    m match {
      case m: CreateAvatar =>
        world.arise[Avatar](id) {
          Avatar(id, name = m.name)
        }
      case m: CreateTreasure =>
        world.arise[Treasure](id) {
          Treasures.create(id = id, treasureId = m.treasureId)
      case m: CreateVillager =>
        world.arise[Villager](id) {
          Villagers.create(id = id, villagerId = m.villagerId)
        }
      case m: CreateCreature =>
        // `arise` returns new world created creature, and new messages to be replied to the original sender
        world.arise[Creature](id) {
          Creatures.create(id = id, creatureId = m.creatureId)
        }
      case m: Projectile =>
        world.arise[Projectile)(id) {
          Projectiles.create(id = id, projectileId = m.projectileId)
        }
      case m: Join =>
        val id = toIdentity(m.id)
        // `occur` returns new world with replaced avatar and new messages
        world.occur(id) { a =>
          // `join` returns
          a.join(world)
        }
      case m: Animate =>
        world.occur[Animator](id) { a =>
         a.animate(m.animationId)
        }
      case m: DealDamage =>
        // `occur` returns new world with replaced attacker and target, new messages`
        world.occur[Attacker,Target](toIdentity(m.attackerId), toIdentity(m.targetId)) { (a, b) =>
          // `attack` returns
          a.attack(b)
        }
    }

    object Animation {
      val animations = List(
        Animation(1, "attack1")
        Animation(2, "attack2")
        Animation(3, "spell1")
      )
    }

    object Projectile {
      val projectiles = List(
        Projectile(1, "FireBall")
      )
    }

  */

}
