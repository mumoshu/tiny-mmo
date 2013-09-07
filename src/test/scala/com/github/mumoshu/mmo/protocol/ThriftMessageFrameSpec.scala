package com.github.mumoshu.mmo.protocol

import org.specs2.mutable._
import akka.io.{PipelineContext, PipelineFactory}
import akka.testkit.{TestKit, ImplicitSender}
import akka.actor.{Actor, ActorSystem}
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._
import akka.util.ByteString
import com.github.mumoshu.mmo.thrift.message.Join

class ThriftMessageFrameSpec extends Specification with NoTimeConversions {

  sequential

  class TestKitEnvironment(_system: ActorSystem = ActorSystem("ThriftMessageFrameSpec"))
    extends TestKit(_system) with Scope {

  }

  "The ThriftMessageFrame pipeline" should {

    "serialize messages from the server and serialize the messages from clients" in new TestKitEnvironment {

      val ctx = new PipelineContext {
      }

      val pipeline = PipelineFactory.buildWithSinkFunctions(ctx,
        new ThriftMessageFrame
      )(
        cmd ⇒ testActor ! cmd.get,
        evt ⇒ testActor ! evt.get)

      val originalCmd = new Join("mumoshu")

      pipeline.injectCommand(originalCmd)

      val belowCmd = receiveOne(0 seconds).asInstanceOf[(Byte, Array[Byte])]

      pipeline.injectEvent(belowCmd)

      val aboveEvt = receiveOne(0 seconds)

      aboveEvt should beEqualTo (originalCmd)
    }
  }

}
