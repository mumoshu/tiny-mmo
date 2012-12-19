package org.example.models

import org.specs2.mutable._
import world.world._

object InMemoryWorldSpec extends Specification {

  val initialWorld: InMemoryWorld = new InMemoryWorld(List.empty, Terrain(
    Array(
      Array(Tile.Ground, Tile.Ground),
      Array(Tile.Ground, Tile.Ground)
    )
  ))

  "The world" should {

    "allow user joining" in {

      var world = initialWorld
      val player = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))

      world = world.join(player)

      world.find(player.id) must be some

    }

    "allow moving things relatively" in {
      var world = initialWorld
      val a = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))
      world = world.appear(a)
      val (newWorld, newThing) = world.tryMove(a, Movement(Position(1f, 1f)))
      newWorld.find(a.id).get.position must be equalTo (Position(1f + 1f, 2f + 1f))
    }

    "allow moving things absolutely" in {
      var world = initialWorld
      val a = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))
      world = world.appear(a)
      val (newWorld, newThing) = world.tryMoveTo(a, Position(1f, 1f))
      newWorld.find(a.id).get.position must be equalTo (Position(1f, 1f))
    }

    "allow attacking" in {
      var world = initialWorld
      val a = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))
      val b = new LivingPlayer(StringIdentity("two"), 1f, Position(2f, 2f))
      val (newWorld, atk, targ) = initialWorld.join(a).join(b).attack(a,b)
      targ must beAnInstanceOf[DiedPlayer]
    }
  }

}
