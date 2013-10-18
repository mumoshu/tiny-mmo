package com.github.mumoshu.mmo.protocol

import akka.io.{SymmetricPipePair, PipelineContext, SymmetricPipelineStage}
import java.nio.ByteOrder
import akka.util.ByteString
import scala.annotation.tailrec

class LengthFieldFrame(maxSize: Int,
                       byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
                       headerSize: Int = 4,
                       lengthIncludesHeader: Boolean = false)
  extends SymmetricPipelineStage[PipelineContext, ByteString, ByteString] {

  // range checks omitted ...

  override def apply(ctx: PipelineContext) =
    new SymmetricPipePair[ByteString, ByteString] {
      var buffer = None: Option[ByteString]
      implicit val byteOrder = LengthFieldFrame.this.byteOrder

      /**
       * Extract as many complete frames as possible from the given ByteString
       * and return the remainder together with the extracted frames in reverse
       * order.
       */
      @tailrec
      def extractFrames(bs: ByteString, acc: List[ByteString]) //
      : (Option[ByteString], Seq[ByteString]) = {
        println(s"DATA_TO_EXTRACT_FRAMES: ${bs}")
        if (bs.isEmpty) {
          (None, acc)
        } else if (bs.length < headerSize) {
          (Some(bs.compact), acc)
        } else {
          val length = bs.iterator.getLongPart(headerSize).toInt
          if (length < 0 || length > maxSize)
            throw new IllegalArgumentException(
              s"received too large frame of size $length (max = $maxSize)")
          val total = if (lengthIncludesHeader) length else length + headerSize
          if (bs.length >= total) {
            extractFrames(bs drop total, bs.slice(headerSize, total) :: acc)
          } else {
            (Some(bs.compact), acc)
          }
        }
      }

      /*
       * This is how commands (writes) are transformed: calculate length
       * including header, write that to a ByteStringBuilder and append the
       * payload data. The result is a single command (i.e. `Right(...)`).
       */
      override def commandPipeline =
      { bs: ByteString ⇒
        val length =
          if (lengthIncludesHeader) bs.length + headerSize else bs.length
        if (length > maxSize) Seq()
        else {
          val bb = ByteString.newBuilder
          bb.putLongPart(length, headerSize)
          bb ++= bs
          ctx.singleCommand(bb.result)
        }
      }

      /*
       * This is how events (reads) are transformed: append the received
       * ByteString to the buffer (if any) and extract the frames from the
       * result. In the end store the new buffer contents and return the
       * list of events (i.e. `Left(...)`).
       */
      override def eventPipeline =
      { bs: ByteString ⇒
        val data = if (buffer.isEmpty) bs else buffer.get ++ bs
        println(s"DATA: ${data}")
        val (nb, frames) = extractFrames(data, Nil)
        buffer = nb
        /*
         * please note the specialized (optimized) facility for emitting
         * just a single event
         */
        frames match {
          case Nil        ⇒ Nil
          case one :: Nil ⇒ ctx.singleEvent(one)
          case many       ⇒ many reverseMap (Left(_))
        }
      }
    }
}
