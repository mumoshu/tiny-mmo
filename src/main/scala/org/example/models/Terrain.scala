package org.example.models

case class Tile(number: Int)

object Tile {
  val Ground = Tile(1)
  // in meters
  val tileWidth = 10.0
  val tileHeight = 10.0

}

case class Terrain(data: Array[Array[Tile]]) {
  def width: Double = data.map(_.size * Tile.tileWidth).max
  def height: Double = data.transpose.map(_.size * Tile.tileHeight).max
}

object DefaultTerrain extends Terrain(
  Array(
    Array(Tile.Ground, Tile.Ground),
    Array(Tile.Ground, Tile.Ground)
  )
)
