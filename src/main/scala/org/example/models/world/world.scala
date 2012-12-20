package org.example.models.world.world

import org.example.models.Terrain

sealed trait Identity

case class StringIdentity(id: String) extends Identity

case class Position(x: Float, z: Float) {
  def move(m: Movement) = copy(x = x + m.diff.x, z = z + m.diff.z)
}

case class Movement(diff: Position)

/**
 * 動物または静物といった「物」
 */
sealed trait Thing {
  val id: Identity

  /**
   * 移動先に何もなければ移動する
   * @param movement
   * @param world
   * @return
   */
  def tryMove(movement: Movement)(implicit world: World): Thing = {
    val simulated = move(movement)
    if (world.existsAt(simulated.position))
      this
    else
      simulated
  }

  def tryMoveTo(p: Position)(implicit world: World): Thing = {
    if (world.existsAt(p))
      this
    else
      moveTo(p)
  }

  /**
   * 現在位置
   * @return
   */
  def position: Position

  /**
   * 移動先に何があったとしても強制的に移動する
   * @param movement
   * @return
   */
  def move(movement: Movement): Thing

  def moveTo(p: Position): Thing

}

/**
 * エネミーやプレイヤーなどの生物
 */
sealed trait Living extends Thing {

  val life: Float

}

/**
 * LivingThingのDeadBody
 */
sealed trait Died extends Thing
sealed trait Player
sealed trait Attacker extends Living
sealed trait Target extends Living {
  /**
   * ダメージをうける
   * @return
   */
  def reduceLife(reducedLife: Float): Either[Living, Died]
}

// プレイヤーは何もしていないか
case class LivingPlayer(id: Identity, life: Float, position: Position) extends Living with Player with Attacker with Target {
  /**
   * 移動先に何があったとしても強制的に移動する
   * @param movement
   * @return
   */
  def move(movement: Movement) = copy(position = position.move(movement))

  def moveTo(p: Position) = copy(position = p)

  def reduceLife(reducedLife: Float) = {
    val lifeAfter: Float = life - reducedLife
    if (lifeAfter > 0)
      Left(copy(life = lifeAfter))
    else
      Right(DiedPlayer(id = id, position = position))
  }
}
//// 攻撃しているか
//case class AttackingPlayer(id: Identity, life: Float, position: Position) extends Living with Player with Attacker{
//  /**
//   * 移動先に何があったとしても強制的に移動する
//   * @param movement
//   * @return
//   */
//  def move(movement: Movement) = copy(position = position.move(movement))
//}
//// 攻撃を受けているか
//case class TargetPlayer(id: Identity, life: Float, position: Position) extends Living with Player with Target{
//  /**
//   * 移動先に何があったとしても強制的に移動する
//   * @param movement
//   * @return
//   */
//  def move(movement: Movement) = copy(position = position.move(movement))
//
//  /**
//   * ダメージをうける
//   * @return
//   */
//  def reduceLife(reducedLife: Float) = {
//    val lifeAfter: Float = life - reducedLife
//    if (lifeAfter > 0)
//      Left(copy(life = lifeAfter))
//    else
//      Right(DiedPlayer(id = id, position = position))
//  }
//}
// 死んでいる
case class DiedPlayer(id: Identity, position: Position) extends Player with Died{
  /**
   * 移動先に何があったとしても強制的に移動する
   * @param movement
   * @return
   */
  def move(movement: Movement) = copy(position = position.move(movement))
  def moveTo(p: Position) = copy(position = p)
}

trait World {

  def say(p: Thing, text: String): World

  def existsAt(position: Position): Boolean

  def appear(t: Thing): World

  def disappear(t: Thing): World

  def attack(attacker: Attacker, target: Target): (World, Attacker, Thing)

  def findExcept(id: Identity): List[Thing]

  def find(id: Identity): Option[Thing]

  def tryMove(livingThing: Thing, movement: Movement): (World, Thing)

  def tryMoveTo(t: Thing, p: Position): (World, Thing)

  def join(p: LivingPlayer): World

  def leave(p: LivingPlayer): World
}

trait ConnectedWorld extends World {

  abstract override def attack(attacker: Attacker, target: Target): (World, Attacker, Thing) = {
    // TODO Send this event to nearby players
    super.attack(attacker, target)
  }

}

sealed trait Speech {
  /**
   * Who
   */
  val id: Identity
  val text: String
}

case class Say(id: Identity, text: String) extends Speech
case class Shout(id: Identity, text: String) extends Speech

class InMemoryWorld(val things: List[Thing], val terrain: Terrain, val speeches: List[Speech]) extends World {
  def say(p: Thing, text: String) =
    new InMemoryWorld(things = things, terrain = terrain, speeches = speeches :+ Say(p.id, text))

  def appear(t: Thing) =
    new InMemoryWorld(things = things :+ t, terrain = terrain, speeches = speeches)

  def disappear(t: Thing) =
    new InMemoryWorld(things = things.filter(_ == t), terrain = terrain, speeches = speeches)

  def attack(attacker: Attacker, target: Target) = {
    // TODO atk - def
    val dealtDmg = 1
    val thingsAfter = things.map {
      case t if t == target =>
        target.reduceLife(dealtDmg).merge
      case t =>
        t
    }
    val t2 = thingsAfter.find(_.id == target.id).getOrElse {
      throw new RuntimeException("Target not found.")
    }
    (
      new InMemoryWorld(things = thingsAfter, terrain = terrain, speeches = speeches),
      attacker,
      t2
    )
  }

  def find(id: Identity) = things.find(_.id == id)

  def findExcept(id: Identity) = things.filterNot(_.id == id)

  def join(p: LivingPlayer) = new InMemoryWorld(things = things :+ p, terrain = terrain, speeches = speeches)

  def leave(p: LivingPlayer) = new InMemoryWorld(things = things.filter(_.id != p.id), terrain = terrain, speeches = speeches)

  def tryMove(thing: Thing, movement: Movement) = (
    new InMemoryWorld(
      things = things.map {
        case t if t.id == thing.id =>
          t.tryMove(movement)(this)
        case t =>
          t
      },
      terrain = terrain,
      speeches = speeches
    ),
    thing
  )

  def existsAt(position: Position) = things.exists(t =>
    Math.sqrt(Math.pow(position.x - t.position.x, 2) + Math.pow(position.z - t.position.z, 2)) < 1.0f
  )

  def tryMoveTo(t: Thing, p: Position) = {
    val moved = t.tryMoveTo(p)(this)
    (
      new InMemoryWorld(
        things = things.map {
          case tt if tt.id == t.id =>
            moved
          case tt =>
            tt
        },
        terrain = terrain,
        speeches = speeches
      ),
      moved
    )
  }
}
