package org.example.models

import com.mongodb.casbah.Imports._
import org.apache.commons.codec.binary.Base64

case class Player(id: String, nickname: String)

case class Id(id: String) {
  override def toString = id
  def toByteArray = Id.strToBytes(id)
}

object Id {

  def bytesToStr(bytes: Array[Byte]): String = Base64.encodeBase64String(bytes)
  def strToBytes(str: String): Array[Byte] = Base64.decodeBase64(str)

  def fromByteArray(bytes: Array[Byte]): Id = Id(bytesToStr(bytes))
  def fromString(idStr: String): Id = Id(idStr)

}

object MongoBackedWorld {

  val connection = MongoConnection("localhost")
  val db = connection("runner")
  val collection = db("players")

  def join(id: String, nickname: String): Player =
    join(Player(id, nickname))

  def join(p: Player) = {
    collection.insert(DBObject(
      "id" -> p.id,
      "nickname" -> p.nickname
    ))
    p
  }

  def findExcept(id: String): List[Player] = {
    collection.find(DBObject("id" -> DBObject("$ne" -> id))).map {
      dbobj => {
        val id = dbobj.getAs[String]("id").get
        val nickname = dbobj.getAs[String]("nickname").get
        Player(id = id, nickname = nickname)
      }
    }.toList
  }

  def tryMove(id: String, x: Double) {
    collection.update(
      DBObject(
        "id" -> id
      ),
      DBObject(
        "$set" -> DBObject(
          "x" -> x
        )
      )
    )
  }

  def leave(id: String) {
    collection.remove(DBObject(
      "id" -> id
    ))
  }
}
