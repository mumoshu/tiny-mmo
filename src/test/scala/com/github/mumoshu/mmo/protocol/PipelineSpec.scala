package com.github.mumoshu.mmo.protocol

import org.specs2.mutable._
import akka.io.{PipelineContext, PipelineFactory}
import akka.testkit.{TestKit, ImplicitSender}
import akka.actor.{Actor, ActorSystem}
import com.github.mumoshu.mmo.thrift.message.Join
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._
import akka.util.ByteString

class PipelineSpec extends Specification with NoTimeConversions {

  sequential

  class TestKitEnvironment(_system: ActorSystem = ActorSystem("PipelineSpec"))
    extends TestKit(_system) with Scope {

  }

  "The pipeline" should {

    "serialize messages from the server and serialize the messages from clients" in new TestKitEnvironment {

      val ctx = new PipelineContext {
      }

      val pipeline = PipelineFactory.buildWithSinkFunctions(ctx,
        new ThriftMessageFrame >>
          new HintFieldFrame >>
          new LengthFieldFrame(10000)
      )(
        cmd ⇒ testActor ! cmd.get,
        evt ⇒ testActor ! evt.get)

      val originalCmd = new Join("mumoshu")

      pipeline.injectCommand(originalCmd)

      val belowCmd = receiveOne(0 seconds).asInstanceOf[ByteString]

      pipeline.injectEvent(belowCmd)

      val aboveEvt = receiveOne(0 seconds)

      aboveEvt should beEqualTo (originalCmd)
    }
  }

}
