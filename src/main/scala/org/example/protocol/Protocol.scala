package org.example.protocol

import akka.zeromq.{Frame, ZMQMessage}
import org.apache.thrift.transport.TIOStreamTransport
import org.apache.thrift.protocol.TBinaryProtocol

trait Protocol {

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

  // org.example.ZMQServer.deserialize(akka.zeromq.ZMQMessage(Seq(akka.zeromq.Frame(Seq(1:Byte)), akka.zeromq.Frame(Seq.empty[Byte]), akka.zeromq.Frame(Seq(0:Byte)), akka.zeromq.Frame(Seq(11,0,1,0,0,0,4,104,111,103,101,0).map(_.toByte)))))
  def deserialize(m: Payload): AnyRef = {
    m match {
      case codec(hint, bytes) =>

          val bais = new java.io.ByteArrayInputStream(bytes)
          val transport = new TIOStreamTransport(bais)
          val protocol = new TBinaryProtocol(transport)

          hint match {
            case 0 =>
              val d = new serializers.thrift.Join()
              d.read(protocol)
              d
            case 1 =>
              val d = new serializers.thrift.Move()
              d.read(protocol)
              d
            case 2 =>
              val d = new serializers.thrift.Leave()
              d.read(protocol)
              d
            case 3 =>
              val d = new serializers.thrift.Respawn()
              d.read(protocol)
              d
            case 4 =>
              val d = new serializers.thrift.Joined()
              d.read(protocol)
              d
            case 5 =>
              val d = new serializers.thrift.Moved()
              d.read(protocol)
              d
            case 6 =>
              val d = new serializers.thrift.Left()
              d.read(protocol)
              d
            case 7 =>
              val d = new serializers.thrift.Respawned()
              d.read(protocol)
              d
            case unexpected =>
              throw new RuntimeException("Unexpected hint: " + hint)
          }
    }

  }

  // org.example.ZMQServer.serialize(org.example.ZMQServer.Message(akka.zeromq.Frame(Seq(1:Byte)), new serializers.thrift.Join("hoge")))
  def serialize(m: AnyRef): Payload = {
    val baos = new java.io.ByteArrayOutputStream()
    val transport = new TIOStreamTransport(baos)
    val protocol = new TBinaryProtocol(transport)
    def comp(hint: Byte) = {
      codec(hint, baos.toByteArray)
    }
    m match {
      case b: serializers.thrift.Join =>
        b.write(protocol)
        comp(0)
      case b: serializers.thrift.Move =>
        b.write(protocol)
        comp(1)
      case b: serializers.thrift.Leave =>
        b.write(protocol)
        comp(2)
      case b: serializers.thrift.Respawn =>
        b.write(protocol)
        comp(3)
      case b: serializers.thrift.Joined =>
        b.write(protocol)
        comp(4)
      case b: serializers.thrift.Moved =>
        b.write(protocol)
        comp(5)
      case b: serializers.thrift.Left =>
        b.write(protocol)
        comp(6)
      case b: serializers.thrift.Respawned =>
        b.write(protocol)
        comp(7)
      case unexpected =>
        throw new RuntimeException("Couldn't serialize an unexpected body: " + m)
    }
  }

}
