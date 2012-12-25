package com.github.mumoshu.mmo.testing.bot

import com.github.mumoshu.mmo.models.world.world.Identity

case object AskForMyId
case object FindAllThings

case class GetPosition(id: Identity)
case class Send(message: AnyRef)
