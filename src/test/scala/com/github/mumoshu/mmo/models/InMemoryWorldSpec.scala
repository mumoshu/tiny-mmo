package com.github.mumoshu.mmo.models

import org.specs2.mutable._
import world.world._
import org.specs2.mock.Mockito
import org.specs2.specification.Scope

object InMemoryWorldSpec extends Specification {

  class world extends Scope with Mockito {

    val id = StringIdentity("one")

    val changeLogger = mock[WorldChangeHandler]

    val initialWorld: InMemoryWorld = new InMemoryWorld(
      List.empty,
      Terrain(
        Array(
          Array(Tile.Ground, Tile.Ground),
          Array(Tile.Ground, Tile.Ground)
        )
      ),
      List.empty, changeLogger)

  }


  "The world" should {

    "allow user joining" in new world {

      var world = initialWorld
      val player = new LivingPlayer(id, 10f, Position(1f, 2f))

      changeLogger.joined(id, "someone") returns changeLogger

      world = world.join(player)

      world.find(player.id) must be some

      there was one(changeLogger).joined(id, "someone")

    }

    "allow user leaving" in new world {

      val player = new LivingPlayer(id, 10f, Position(1f, 2f))

      changeLogger.joined(id, "someone") returns changeLogger
      changeLogger.left(id) returns changeLogger

      initialWorld.join(player).leave(player).find(player.id) must be none

      there was one(changeLogger).joined(id, "someone") then
        one(changeLogger).left(id)
    }

    "allow finding an user" in new world {

      val player = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))

      changeLogger.joined(id, "someone") returns changeLogger

      initialWorld.join(player).find(player.id) must be some

      there was one(changeLogger).joined(id, "someone")
    }

    "allow appearing a thing" in new world {
      val house = new StaticObject(StringIdentity("house1"), "house", Position(1f, 2f))

      initialWorld.appear(house).things must be size(1)
    }

    "allow disappearing a thing" in new world {
      val house = new StaticObject(StringIdentity("house1"), "house", Position(1f, 2f))

      initialWorld.appear(house).disappear(house).things must be size(0)
    }

    "allow moving things relatively" in new world {
      var world = initialWorld
      val a = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))
      world = world.appear(a)
      val (newWorld, newThing) = world.tryMove(a, Movement(Position(1f, 1f)))
      newWorld.find(a.id).get.position must be equalTo (Position(1f + 1f, 2f + 1f))
    }

    "allow moving things absolutely" in new world {
      var world = initialWorld
      val a = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))
      world = world.appear(a)
      val (newWorld, newThing) = world.tryMoveTo(a, Position(1f, 1f))
      newWorld.find(a.id).get.position must be equalTo (Position(1f, 1f))
    }

    "allow attacking" in new world {
      var world = initialWorld
      val a = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))
      val b = new LivingPlayer(StringIdentity("two"), 1f, Position(2f, 2f))

      changeLogger.joined(a.id, "someone") returns changeLogger
      changeLogger.joined(b.id, "someone") returns changeLogger
      changeLogger.attacked(a.id, b.id) returns changeLogger

      val (newWorld, atk, targ) = initialWorld.join(a).join(b).attack(a,b)
      targ must beAnInstanceOf[DiedPlayer]

      there was one(changeLogger).joined(a.id, "someone") then
        one(changeLogger).joined(b.id, "someone") then
        one(changeLogger).attacked(a.id, b.id)
    }

    "allow to say something" in new world {
      val a = new LivingPlayer(StringIdentity("one"), 10f, Position(1f, 2f))

      changeLogger.said(a.id, "hi") returns changeLogger

      initialWorld.say(a, "hi").speeches.size must be equalTo (1)

      there was one(changeLogger).said(a.id, "hi")
    }
  }

}
