# PandaDB v0.1
Intelligent Graph Database (import from GraiphDB https://github.com/grapheco/graiphdb)

* single machine
* intelligent property graph model
* cypher plus

# connecting to PandaServer

first, import `connector` as dependency:
```
<dependency>
    <groupId>pandadb</groupId>
    <artifactId>connector</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

then, use `RemotePandaServer.connect`:
```
val client = RemotePandaServer.connect("bolt://localhost:7687");
val (node, name, age, bytes) = client.querySingleObject("match (n) where n.name='bob' return n, n.name, n.age, n.bytes", (result: Record) => {
      (result.get("n").asNode(), result.get("n.name").asString(), result.get("n.age").asInt(), result.get("n.bytes").asByteArray())
    });
```

# Licensing
PandaDB v0.1 is an open source product licensed under GPLv3.
