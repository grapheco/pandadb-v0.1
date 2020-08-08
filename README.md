# PandaDB v0.1
Intelligent Graph Database (import from GraiphDB https://github.com/grapheco/graiphdb)

[![GitHub releases](https://img.shields.io/github/release/grapheco/pandadb-v0.1.svg)](https://github.com/grapheco/pandadb-v0.1/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/grapheco/pandadb-v0.1/total.svg)](https://github.com/grapheco/pandadb-v0.1/releases)
[![GitHub issues](https://img.shields.io/github/issues/grapheco/pandadb-v0.1.svg)](https://github.com/grapheco/pandadb-v0.1/issues)
[![GitHub forks](https://img.shields.io/github/forks/grapheco/pandadb-v0.1.svg)](https://github.com/grapheco/pandadb-v0.1/network)
[![GitHub stars](https://img.shields.io/github/stars/grapheco/pandadb-v0.1.svg)](https://github.com/grapheco/pandadb-v0.1/stargazers)

* single machine
* intelligent property graph model
* cypher plus

## <a name='BuildingPandaDB'></a>Building PandaDB

```
mvn clean install
```

this will install all artifacts in local maven repository.

## <a name='Quickstart'></a>Quick start

### <a name='STEP1.downloadpackage'></a>STEP 1. download package
visit https://github.com/grapheco/pandadb-v0.1/releases to get pandadb-v0.1 binary distributions.

unpack `pandadb-server-x.x.zip` in your local directory, e.g. `/usr/local/`.

`cd /usr/local/pandadb-server-x.x`

### <a name='STEP2.startserver'></a>STEP 2. start server

* `bin/neo4j console`: start a PandaDB server
* `bin/neo4j start`: start a PandaDB server silently

Once successfully startup, infos will be shown as below:

```
2020-08-08 04:20:51.309+0000 INFO  ======== PandaDB (+Neo4j-3.5.6-BLOB) ======== 

 ______                _       _____   ______
(_____ \              | |     (____ \ (____  \
 _____) )___ ____   _ | | ____ _   \ \ ____)  )
|  ____/ _  |  _ \ / || |/ _  | |   | |  __  (
| |   ( ( | | | | ( (_| ( ( | | |__/ /| |__)  )
|_|    \_||_|_| |_|\____|\_||_|_____/ |______/

PandaDB Server (ver 0.1.0.20200801)

2020-08-08 04:20:51.317+0000 INFO  Starting...
[12:20:51:372] DEBUG ExtendedDatabaseLifecyclePluginsService :: loading database lifecycle plugin: cn.pandadb.database.SemanticOperatorPlugin@2f74900b
[12:20:51:372] DEBUG ExtendedDatabaseLifecyclePluginsService :: loading database lifecycle plugin: org.neo4j.kernel.impl.blob.BlobStoragePlugin@27be17c8
[12:20:51:373] DEBUG ExtendedDatabaseLifecyclePluginsService :: loading database lifecycle plugin: org.neo4j.kernel.impl.blob.RegsterDefaultBlobFunctionsPlugin@2c413ffc
[12:20:51:399] INFO  SemanticOperatorPlugin :: loading semantic plugins: /Users/bluejoe/IdeaProjects/pandadb-v0.1/itest/testinput/cypher-plugins.xml
[12:20:51:523] INFO  BlobStorage$         :: using batch blob storage: org.neo4j.kernel.impl.blob.BlobStorage$DefaultLocalFileSystemBlobValueStorage@2ecf5915
[12:20:51:650] DEBUG ConfigurationEx      :: no value set for blob.storage.file.dir, using default: /Users/bluejoe/IdeaProjects/pandadb-v0.1/itest/./testoutput/testdb/data/databases/graph.db/blob
[12:20:51:650] INFO  BlobStorage$DefaultLocalFileSystemBlobValueStorage :: using storage dir: /Users/bluejoe/IdeaProjects/pandadb-v0.1/itest/testoutput/testdb/data/databases/graph.db/blob
2020-08-08 04:20:52.527+0000 INFO  Bolt enabled on 0.0.0.0:7687.
2020-08-08 04:20:54.926+0000 INFO  Started.
2020-08-08 04:20:56.224+0000 INFO  Remote interface available at http://localhost:7474/
```

### <a name='STEP3.connectPandaDBServer'></a>STEP 3. connect remote PandaDB

clients communicate with PandaDB via `Cypher` over Bolt protocol.

* `bin/cypher-shell`: open a PandaDB client to a remote server

Also, you may visit `http://localhost:7474`  to browse graph data in `neo4j-browser`.

### <a name='STEP4.queryingOnPandaDB'></a>STEP 4. querying on PandaDB

in `neo4j-browser`, users may input `Cypher` commands to query on GraiphDB.

```
create (bluejoe:Person {name: 'bluejoe', mail:'bluejoe2008@gmail.com', photo: <https://bluejoe2008.github.io/p4.png>}) return bluejoe

```
this command will create a Person node with a BLOB property, which content come from the Web URL. If you like, `<file://...>` or `<ftp://...>` is ok.

in `neo4j-browser`, a BLOB property will be displayed as an image icon:

<img src="https://github.com/grapheco/pandadb-v0.1/blob/master/docs/snapshot1.png?raw=true" width="500">

NOTE: if user/password is required, try default values: `neo4j`/`neo4j`.

## <a name='CypherPlus'></a>CypherPlus

PandaDB enhances `Cypher` grammar, naming CypherPlus. CypherPlus allows writing BLOB literals in query commands, also it allows semantic operations on properties, especially BLOB properties.

### <a name='BLOBliterals'></a>BLOB literals

`BlobLiteral` is defined in Cypher grammar in form of:
`<schema://path>`

Following schema is ok:
* file: local files on server
* http
* https
* ftp
* base64: path should be a BASE64 encoding string, for example: \<base64://dGhpcyBpcyBhbiBleGFtcGxl\> represents a string with content `this is an example`

Next code illustrates how to use blob in Cypher query:
```
return <https://bluejoe2008.github.io/bluejoe3.png>
```

more details, see https://github.com/bluejoe2008/graiph-neo4j/blob/cypher-extension/README.md

### <a name='propertyextration'></a>property extration

```
neo4j@<default_database>> match (n {name:'bluejoe'}) return n.photo->mime, n.car->width;
+------------------------------+
| n.photo->mime | n.car->width |
+------------------------------+
| "image/png"   | 640          |
+------------------------------+
```
retrieving plate number of the car:
```
neo4j@<default_database>> match (n {name:'bluejoe'}) return n.car->plateNumber;
+--------------------+
| n.car->plateNumber |
+--------------------+
| "苏B56789"          |
+--------------------+
```

NOTE: some semantic operation requires an AIPM service at 10.0.86.128 (modify this setting in neo4j.conf), if it is unavailable, exceptions will be thrown:

```
neo4j@<default_database>> match (n {name:'bluejoe'}) return n.car->plateNumber;
Failed connect to http://10.0.86.128:8081
```

### <a name='semanticcomparison'></a>semantic comparison

CypherPlus allows semantic comparison on two properties.

Following example query compares two text:
```
neo4j@<default_database>> return 'abc' :: 'abcd', 'abc' ::jaccard 'abcd', 'abc' ::jaro 'abcd', 'hello world' ::cosine 'bye world';
+--------------------------------------------------------------------------------------------------------+
| 'abc' :: 'abcd'    | 'abc' ::jaccard 'abcd' | 'abc' ::jaro 'abcd' | 'hello world' ::cosine 'bye world' |
+--------------------------------------------------------------------------------------------------------+
| 0.9416666805744172 | 0.5                    | 0.9416666805744172  | 0.5039526306789696                 |
+--------------------------------------------------------------------------------------------------------+
```

A good idea is to determine if a person appear in another photo:

```
return <http://s12.sinaimg.cn/mw690/005AE7Quzy7rL8kA4Nt6b&690> ~:0.5 <http://s15.sinaimg.cn/mw690/005AE7Quzy7rL8j2jlIee&690>
```

## <a name='developersmanual'></a>developers' manual
### <a name='connectingremotePandaDB'></a>connecting remote PandaDB

import `pandadb:connector` dependency first:
```
   <dependency>
      <groupId>pandadb</groupId>
      <artifactId>connector</artifactId>
      <version>0.1.0-SNAPSHOT</version>
   </dependency>
```
use object `RemotePandaServer.connect()`:

* def connect(url: String, user: String = "", pass: String = ""): CypherService

`CypherService` has a set of methods:
* def queryObjects[T: ClassTag](queryString: String, fnMap: (Record => T)): Iterator[T]

* def queryObjects[T: ClassTag](queryString: String, params: Map[String, AnyRef], fnMap: (Record => T)): Iterator[T]

* def execute[T](f: (Session) => T): T;

* def executeQuery[T](queryString: String, fn: (StatementResult => T)): T;

* def executeQuery[T](queryString: String, params: Map[String, AnyRef], fn: (StatementResult => T)): T;

* def executeUpdate(queryString: String);

* def executeUpdate(queryString: String, params: Map[String, AnyRef])

* def querySingleObject[T](queryString: String, fnMap: (Record => T)): T

* def querySingleObject[T](queryString: String, params: Map[String, AnyRef], fnMap: (Record => T)): T

A simple example:
```
    val conn = RemotePandaServer.connect("bolt://localhost:7687", "neo4j", "123");
    //a non-blob
    val (node, name, age) = conn.querySingleObject("match (n) where n.name='bob' return n, n.name, n.age", (result: Record) => {
      (result.get("n").asNode(), result.get("n.name").asString(), result.get("n.age").asInt())
    });
```

more example code, see https://github.com/grapheco/graiph-dist/tree/master/graiph-client-test

### <a name='usingembeddeddatabase'></a>using embedded GraphDB

import `pandadb:database` dependency first:

```
   <dependency>
      <groupId>pandadb</groupId>
      <artifactId>database</artifactId>
      <version>0.1.0-SNAPSHOT</version>
   </dependency>
```

using object `PandaDB`:

* def openDatabase(dbDir: File, propertiesFile: File): GraphDatabaseService

An example of `openDatabase`:
```
   val db = PandaDB.openDatabase(new File("./testdb"), new File("./neo4j.conf"));
   val tx = db.beginTx();
   //create a node
   val node1 = db.createNode();

   node1.setProperty("name", "bob");
   node1.setProperty("age", 40);

   //with a blob property
   node1.setProperty("photo", Blob.fromFile(new File("./testdata/test.png")));
   ...
```

An example of `connect`:
```
   val db = PandaDB.openDatabase(new File("./testdb"), new File("./neo4j.conf"));
   val conn = LocalGraphService.connect(db);
   //a non-blob
    val (node, name, age) = conn.querySingleObject("match (n) where n.name='bob' return n, n.name, n.age", (result: Record) => {
      (result.get("n").asNode(), result.get("n.name").asString(), result.get("n.age").asInt())
    });
```
`LocalGraphService.connect()` returns a `CypherService` too, just like that of `RemotePandaServer.connect()`.

more example code, see https://github.com/grapheco/graiph-dist/tree/master/graiph-database-test

## <a name='handlingBLOBs'></a>handling BLOBs

graiph-neo4j enhances Neo4j with a set of blob operation functions which makes it possible and convenient to store and use the BLOB in neo4j.

more details, see https://github.com/bluejoe2008/graiph-neo4j/blob/cypher-extension/blob.md

# Licensing
PandaDB v0.1 is an open source product licensed under GPLv3.
