namespace java serializers.thrift
namespace csharp serializers.thrift

typedef i32 int
typedef i64 long

struct Join {
  1: string name,
}

struct Start {

}

struct Forward {
    1: double dx,
}

struct Die {

}

struct Respawn {

}

struct Leave {

}
