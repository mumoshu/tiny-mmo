namespace java com.github.mumoshu.mmo.thrift.message
namespace csharp Thrift.Messages

typedef i32 int
typedef i64 long

struct Join {
  1: string name,
}

struct Start {

}

struct Move {
  1: double x,
}

struct Die {
  1: double x,
}

struct Respawn {
  1: double x,
}

struct Leave {

}

struct Joined {
  1: string id,
}

struct Left {
  1: string id,
}

struct Moved {
  1: string id,
  2: double x
}

struct Died {
  1: string id,
  2: double x
}

struct Respawned {
  1: string id,
  2: double x
}

struct Say {
  1: string id,
  2: string text,
}

struct Shout {
  1: string id,
  2: string text
}

struct Attack {
  1: string attackerId,
  2: string targetId
}

struct Appear {
  1: string id,
  2: double x,
  3: double z,
}

struct Disappear {
  1: string id,
}

struct MoveTo {
  1: string id,
  2: double x,
  3: double z,
}

struct GetPosition {
  1: string id,
}

struct Position {
  1: string id,
  2: double x,
  3: double z
}

struct Vector3 {
  1: double x,
  2: double y,
  3: double z,
}

struct MyId {
}

struct YourId {
  1: string id,
}

struct FindAllThings {

}

struct Thing {
  1: string id,
  2: Position position,
}

struct Things {
  1: list<Thing> ts,
}

struct Presentation {
  1: string id,
  2: string ownerId,
  3: string url,
  4: Vector3 position,
  5: Vector3 rotation,
  6: Vector3 zoom
}
