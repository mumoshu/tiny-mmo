package com.github.mumoshu.mmo.protocol

import akka.io.{SymmetricPipePair, PipelineContext, SymmetricPipelineStage}
import java.nio.ByteOrder
import akka.util.ByteString
import org.apache.thrift.TBase
import org.apache.thrift.transport.TIOStreamTransport
import org.apache.thrift.protocol.TBinaryProtocol
import com.github.mumoshu.mmo.thrift
import scala.util.{Success, Failure, Try}
import scala.util.control.NonFatal
import org.slf4j.LoggerFactory

class ThriftMessageFrame(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN)
  extends SymmetricPipelineStage[PipelineContext, AnyRef, (Byte, Array[Byte])] {

  val log2 = LoggerFactory.getLogger(this.getClass)

  override def apply(ctx: PipelineContext) =
    new SymmetricPipePair[AnyRef, (Byte, Array[Byte])] {
      implicit val byteOrder = ThriftMessageFrame.this.byteOrder

      override def commandPipeline =
      { m: AnyRef ⇒

        val baos = new java.io.ByteArrayOutputStream()
        val transport = new TIOStreamTransport(baos)
        val protocol = new TBinaryProtocol(transport)
        def messageInBytesWithTypeHint(hint: Byte) = {
          (hint, baos.toByteArray)
        }

        Try {
          m match {
            case b: thrift.message.Join =>
              b.write(protocol)
              messageInBytesWithTypeHint(0)
            case b: thrift.message.Move =>
              b.write(protocol)
              messageInBytesWithTypeHint(1)
            case b: thrift.message.Leave =>
              b.write(protocol)
              messageInBytesWithTypeHint(2)
            case b: thrift.message.Respawn =>
              b.write(protocol)
              messageInBytesWithTypeHint(3)
            case b: thrift.message.Joined =>
              b.write(protocol)
              messageInBytesWithTypeHint(4)
            case b: thrift.message.Moved =>
              b.write(protocol)
              messageInBytesWithTypeHint(5)
            case b: thrift.message.Left =>
              b.write(protocol)
              messageInBytesWithTypeHint(6)
            case b: thrift.message.Respawned =>
              b.write(protocol)
              messageInBytesWithTypeHint(7)
            case b: thrift.message.Attack =>
              b.write(protocol)
              messageInBytesWithTypeHint(8)
            case b: thrift.message.MyId =>
              b.write(protocol)
              messageInBytesWithTypeHint(9)
            case b: thrift.message.YourId =>
              b.write(protocol)
              messageInBytesWithTypeHint(10)
            case b: thrift.message.GetPosition =>
              b.write(protocol)
              messageInBytesWithTypeHint(11)
            case b: thrift.message.Position =>
              b.write(protocol)
              messageInBytesWithTypeHint(12)
            case b: thrift.message.MoveTo =>
              b.write(protocol)
              messageInBytesWithTypeHint(13)
            case b: thrift.message.Say =>
              b.write(protocol)
              messageInBytesWithTypeHint(14)
            case b: thrift.message.Shout =>
              b.write(protocol)
              messageInBytesWithTypeHint(15)
            case b: thrift.message.Appear =>
              b.write(protocol)
              messageInBytesWithTypeHint(16)
            case b: thrift.message.Disappear =>
              b.write(protocol)
              messageInBytesWithTypeHint(17)
            case b: thrift.message.FindAllThings =>
              b.write(protocol)
              messageInBytesWithTypeHint(18)
            case b: thrift.message.Things =>
              b.write(protocol)
              messageInBytesWithTypeHint(19)
            case unexpected =>
              throw new RuntimeException("Couldn't serialize an unexpected body: " + m + "(" + m.getClass + ")")
          }
        } match {
          case Success(belowCmd) =>
            ctx.singleCommand(belowCmd)
          case Failure(NonFatal(e)) =>
            log2.error("Exception while serializing a messsage.", e)
            ctx.nothing
        }
      }

      override def eventPipeline =
      { bs: (Byte, Array[Byte]) ⇒
        val hint = bs._1
        val bytes = bs._2
        val bais = new java.io.ByteArrayInputStream(bytes)
        val transport = new TIOStreamTransport(bais)
        val protocol = new TBinaryProtocol(transport)

        Try {

          val above = hint match {
            case 0 =>
              val d = new thrift.message.Join()
              d.read(protocol)
              d
            case 1 =>
              val d = new thrift.message.Move()
              d.read(protocol)
              d
            case 2 =>
              val d = new thrift.message.Leave()
              d.read(protocol)
              d
            case 3 =>
              val d = new thrift.message.Respawn()
              d.read(protocol)
              d
            case 4 =>
              val d = new thrift.message.Joined()
              d.read(protocol)
              d
            case 5 =>
              val d = new thrift.message.Moved()
              d.read(protocol)
              d
            case 6 =>
              val d = new thrift.message.Left()
              d.read(protocol)
              d
            case 7 =>
              val d = new thrift.message.Respawned()
              d.read(protocol)
              d
            case 8 =>
              val d = new thrift.message.Attack()
              d.read(protocol)
              d
            case 9 =>
              val d = new thrift.message.MyId()
              d.read(protocol)
              d
            case 10 =>
              val d = new thrift.message.YourId()
              d.read(protocol)
              d
            case 11 =>
              val d = new thrift.message.GetPosition()
              d.read(protocol)
              d
            case 12 =>
              val d = new thrift.message.Position()
              d.read(protocol)
              d
            case 13 =>
              val d = new thrift.message.MoveTo()
              d.read(protocol)
              d
            case 14 =>
              val d = new thrift.message.Say()
              d.read(protocol)
              d
            case 15 =>
              val d = new thrift.message.Shout()
              d.read(protocol)
              d
            case 16 =>
              val d = new thrift.message.Appear()
              d.read(protocol)
              d
            case 17 =>
              val d = new thrift.message.Disappear()
              d.read(protocol)
              d
            case 18 =>
              val d = new thrift.message.FindAllThings
              d.read(protocol)
              d
            case 19 =>
              val d = new thrift.message.Things
              d.read(protocol)
              d
            case unexpected =>
              throw new RuntimeException("Unexpected hint: " + hint)
          }
          val available = bais.available()
          if (available != 0) {
            log2.warn(available + " bytes left", new RuntimeException)
          }
          above
        } match {
          case Success(aboveEvt) =>
            ctx.singleEvent(aboveEvt)
          case Failure(NonFatal(e)) =>
            log2.error("Exception while deserializing a message", e)
            ctx.nothing
        }
      }
    }
}
