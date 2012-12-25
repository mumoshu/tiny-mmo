package com.github.mumoshu.mmo.protocol

import akka.zeromq.{Frame, ZMQMessage}
import org.apache.thrift.transport.TIOStreamTransport
import org.apache.thrift.protocol.TBinaryProtocol
import org.slf4j.LoggerFactory
import com.github.mumoshu.mmo.thrift

trait Protocol {

  val log2 = LoggerFactory.getLogger(this.getClass)

  type Payload

  trait Codec[T] {

    /**
     * Composes the message content into a TransportMessage
     * @param hint
     * @param bytes
     * @return
     */
    def apply(hint: Byte, bytes: Array[Byte]): T

    /**
     * Decompose the TransportMessage and extracts its content
     * @param m the message decomposed
     * @return
     */
    def unapply(m: T): Option[(Byte, Array[Byte])]

  }

  val codec: Codec[Payload]

  // com.github.mumoshu.mmo.ZMQServer.deserialize(akka.zeromq.ZMQMessage(Seq(akka.zeromq.Frame(Seq(1:Byte)), akka.zeromq.Frame(Seq.empty[Byte]), akka.zeromq.Frame(Seq(0:Byte)), akka.zeromq.Frame(Seq(11,0,1,0,0,0,4,104,111,103,101,0).map(_.toByte)))))
  def deserialize(m: Payload): AnyRef = {
    m match {
      case codec(hint, bytes) =>

          val bais = new java.io.ByteArrayInputStream(bytes)
          val transport = new TIOStreamTransport(bais)
          val protocol = new TBinaryProtocol(transport)

          val data = hint match {
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
//              throw new RuntimeException("Unexpected hint: " + hint)
              println("Unexpected hit: " + hint)
              None
          }
          val available = bais.available()
          if (available != 0) {
            log2.warn(available + " bytes left", new RuntimeException)
          }
          data
    }

  }

  // com.github.mumoshu.mmo.ZMQServer.serialize(com.github.mumoshu.mmo.ZMQServer.Message(akka.zeromq.Frame(Seq(1:Byte)), new thrift.message.Join("hoge")))
  def serialize(m: AnyRef): Payload = {
    val baos = new java.io.ByteArrayOutputStream()
    val transport = new TIOStreamTransport(baos)
    val protocol = new TBinaryProtocol(transport)
    def comp(hint: Byte) = {
      codec(hint, baos.toByteArray)
    }
    m match {
      case b: thrift.message.Join =>
        b.write(protocol)
        comp(0)
      case b: thrift.message.Move =>
        b.write(protocol)
        comp(1)
      case b: thrift.message.Leave =>
        b.write(protocol)
        comp(2)
      case b: thrift.message.Respawn =>
        b.write(protocol)
        comp(3)
      case b: thrift.message.Joined =>
        b.write(protocol)
        comp(4)
      case b: thrift.message.Moved =>
        b.write(protocol)
        comp(5)
      case b: thrift.message.Left =>
        b.write(protocol)
        comp(6)
      case b: thrift.message.Respawned =>
        b.write(protocol)
        comp(7)
      case b: thrift.message.Attack =>
        b.write(protocol)
        comp(8)
      case b: thrift.message.MyId =>
        b.write(protocol)
        comp(9)
      case b: thrift.message.YourId =>
        b.write(protocol)
        comp(10)
      case b: thrift.message.GetPosition =>
        b.write(protocol)
        comp(11)
      case b: thrift.message.Position =>
        b.write(protocol)
        comp(12)
      case b: thrift.message.MoveTo =>
        b.write(protocol)
        comp(13)
      case b: thrift.message.Say =>
        b.write(protocol)
        comp(14)
      case b: thrift.message.Shout =>
        b.write(protocol)
        comp(15)
      case b: thrift.message.Appear =>
        b.write(protocol)
        comp(16)
      case b: thrift.message.Disappear =>
        b.write(protocol)
        comp(17)
      case b: thrift.message.FindAllThings =>
        b.write(protocol)
        comp(18)
      case b: thrift.message.Things =>
        b.write(protocol)
        comp(19)
      case unexpected =>
        throw new RuntimeException("Couldn't serialize an unexpected body: " + m + "(" + m.getClass + ")")
    }
  }

}
