package com.github.mumoshu.mmo.protocol

import akka.io.{SymmetricPipePair, PipelineContext, SymmetricPipelineStage}
import java.nio.ByteOrder
import akka.util.ByteString

class HintFieldFrame(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN)
  extends SymmetricPipelineStage[PipelineContext, (Byte, Array[Byte]), ByteString] {

  override def apply(ctx: PipelineContext) =
    new SymmetricPipePair[(Byte, Array[Byte]), ByteString] {
      implicit val byteOrder = HintFieldFrame.this.byteOrder

      override def commandPipeline =
      { bs: (Byte, Array[Byte]) ⇒
        val bb = ByteString.newBuilder
        bb += bs._1
        bb ++= bs._2
        ctx.singleCommand(bb.result)
      }

      override def eventPipeline =
      { bs: ByteString ⇒
        val iterator = bs.iterator
        val data = (iterator.getByte, iterator.toArray)
        ctx.singleEvent(data)
      }
    }
}
