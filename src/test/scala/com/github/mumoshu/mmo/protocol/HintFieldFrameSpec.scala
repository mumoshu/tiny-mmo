package com.github.mumoshu.mmo.protocol

import org.specs2.mutable._
import akka.io.{PipelineContext, PipelineFactory}
import akka.testkit.{TestKit, ImplicitSender}
import akka.actor.{Actor, ActorSystem}
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._
import akka.util.ByteString

class HintFieldFrameSpec extends Specification with NoTimeConversions {

  sequential

  class TestKitEnvironment(_system: ActorSystem = ActorSystem("HintFieldFrameSpec"))
    extends TestKit(_system) with Scope {

  }

  "The HintFieldFrame pipeline" should {

    "serialize messages from the server and serialize the messages from clients" in new TestKitEnvironment {

      val ctx = new PipelineContext {
      }

      val pipeline = PipelineFactory.buildWithSinkFunctions(ctx,
        new HintFieldFrame
      )(
        cmd ⇒ testActor ! cmd.get,
        evt ⇒ testActor ! evt.get)

      val originalCmd = (123.toByte, ByteString("mumoshu").toArray)

      pipeline.injectCommand(originalCmd)

      val belowCmd = receiveOne(0 seconds).asInstanceOf[ByteString]

      pipeline.injectEvent(belowCmd)

      val aboveEvt = receiveOne(0 seconds).asInstanceOf[(Byte, Array[Byte])]

      val actualString = new String(aboveEvt._2, "UTF-8")
      val expectedString = new String(originalCmd._2, "UTF-8")

      actualString must beEqualTo (expectedString)

      aboveEvt._1 should beEqualTo (originalCmd._1)
    }
  }

}
