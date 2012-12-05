namespace java serializers.thrift
namespace csharp serializers.thrift

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
