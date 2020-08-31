<!-- vscode-markdown-toc -->
* 1. [Building PandaDB](#BuildingPandaDB)
	* 1.1. [install all artifacts](#installallartifacts)
	* 1.2. [building server-side distribution zip package](#buildingserver-sidedistributionzippackage)
	* 1.3. [building server-side all-in-one jar package](#buildingserver-sideall-in-onejarpackage)
* 2. [Quick start](#Quickstart)
	* 2.1. [STEP 1. download package](#STEP1.downloadpackage)
	* 2.2. [STEP 2. start server](#STEP2.startserver)
	* 2.3. [STEP 3. connect remote PandaDB](#STEP3.connectremotePandaDB)
	* 2.4. [STEP 4. querying on PandaDB](#STEP4.queryingonPandaDB)
* 3. [CypherPlus](#CypherPlus)
	* 3.1. [BLOB literals](#BLOBliterals)
	* 3.2. [property extration](#propertyextration)
	* 3.3. [semantic comparison](#semanticcomparison)
* 4. [developers' manual](#developersmanual)
	* 4.1. [connecting remote PandaDB](#connectingremotePandaDB)
	* 4.2. [using an embedded GraphDB](#usinganembeddedGraphDB)
* 5. [Licensing](#Licensing)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->
# PandaDB v0.1
Intelligent Graph Database (migrated from GraiphDB https://github.com/grapheco/graiphdb)

[![GitHub releases](https://img.shields.io/github/release/grapheco/pandadb-v0.1.svg)](https://github.com/grapheco/pandadb-v0.1/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/grapheco/pandadb-v0.1/total.svg)](https://github.com/grapheco/pandadb-v0.1/releases)
[![GitHub issues](https://img.shields.io/github/issues/grapheco/pandadb-v0.1.svg)](https://github.com/grapheco/pandadb-v0.1/issues)
[![GitHub forks](https://img.shields.io/github/forks/grapheco/pandadb-v0.1.svg)](https://github.com/grapheco/pandadb-v0.1/network)
[![GitHub stars](https://img.shields.io/github/stars/grapheco/pandadb-v0.1.svg)](https://github.com/grapheco/pandadb-v0.1/stargazers)

* single machine
* intelligent property graph model
* cypher plus

##  1. <a name='BuildingPandaDB'></a>Building PandaDB

###  1.1. <a name='installallartifacts'></a>install all artifacts

```
mvn clean install
```

###  1.2. <a name='buildingserver-sidedistributionzippackage'></a>building server-side distribution zip package
```
cd packaging
mvn package -Pserver-unix-dist
```

or

```
cd packaging
mvn package -Pserver-win-dist
```

this command will create `pandadb-server-<version>.tgz` or `pandadb-server-<version>.zip` in `target` directory.

###  1.3. <a name='buildingserver-sideall-in-onejarpackage'></a>building server-side all-in-one jar package
```
cd packaging
mvn package -Pserver-jar
```

this command will create `pandadb-server-all-in-one-<version>.jar` in `target` directory.

##  2. <a name='Quickstart'></a>Quick start

###  2.1. <a name='STEP1.downloadpackage'></a>STEP 1. download package
visit https://github.com/grapheco/pandadb-v0.1/releases to get pandadb-v0.1 binary distributions.

unpack `pandadb-server-<version>.zip` in your local directory, e.g. `/usr/local/`.

`cd /usr/local/pandadb-server-<version>`

###  2.2. <a name='STEP2.startserver'></a>STEP 2. start server

* `bin/neo4j console`: start a PandaDB server
* `bin/neo4j start`: start a PandaDB server silently

Once PandaDB is successfully startup, infos will be shown as below:

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

###  2.3. <a name='STEP3.connectremotePandaDB'></a>STEP 3. connect remote PandaDB

clients communicate with PandaDB via `Cypher` over Bolt protocol.

* `bin/cypher-shell`: open a PandaDB client to a remote server

Also, you may visit `http://localhost:7474`  to browse graph data in `neo4j-browser`.

###  2.4. <a name='STEP4.queryingonPandaDB'></a>STEP 4. querying on PandaDB

in `neo4j-browser`, users may input `Cypher` commands to query on PandaDB.

```
create (bluejoe:Person {name: 'bluejoe', mail:'bluejoe2008@gmail.com', photo: <https://bluejoe2008.github.io/p4.png>}) return bluejoe

```
this command will create a Person node with a BLOB property, which content come from the Web URL. If you like, `<file://...>` or `<ftp://...>` is ok.

in `neo4j-browser`, a BLOB property will be displayed as an image icon:

<img src="https://github.com/grapheco/pandadb-v0.1/blob/master/docs/snapshot1.png?raw=true" width="500">

NOTE: if user/password is required, try default values: `neo4j`/`neo4j`.

##  3. <a name='CypherPlus'></a>CypherPlus

PandaDB enhances `Cypher` grammar, naming CypherPlus. CypherPlus allows writing BLOB literals in query commands, also it allows semantic operations on properties, especially BLOB properties.

###  3.1. <a name='BLOBliterals'></a>BLOB literals

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

more details, see https://github.com/grapheco/pandadb-v0.1/blob/master/docs/blob.md

###  3.2. <a name='propertyextration'></a>property extration

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

###  3.3. <a name='semanticcomparison'></a>semantic comparison

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

##  4. <a name='developersmanual'></a>developers' manual
###  4.1. <a name='connectingremotePandaDB'></a>connecting an remote PandaDB

import `pandadb:connector` dependency first:
```
   <dependency>
      <groupId>pandadb</groupId>
      <artifactId>connector</artifactId>
      <version>0.1.0-SNAPSHOT</version>
   </dependency>
```

use `GraphDatabase.driver()` to connect remote PandaDB, just like using neo4j:
```
  val _driver = GraphDatabase.driver(url, AuthTokens.basic(user, pass));
  val session = _driver.session()
  val result = session...
  session.close();
  ...
```
    
An alternative way is to use `RemotePandaServer.connect()`:

* def connect(url: String, user: String = "", pass: String = ""): CypherService

it returns a `CypherService` which has a set of methods:
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
    val (node, name, age, photo) = conn.querySingleObject("match (n) where n.name='bob' return n, n.name, n.age", (result: Record) => {
      (result.get("n").asNode(), result.get("n.name").asString(), result.get("n.age").asInt(), result.get("n.photo").asBlob())
    });
```

more example code, see https://github.com/grapheco/pandadb-v0.1/blob/master/itest/src/test/scala/CypherServiceTest.scala

###  4.2. <a name='usinganembeddedGraphDB'></a>using an embedded PandaDB

import `pandadb:database` dependency first:

```
   <dependency>
      <groupId>pandadb</groupId>
      <artifactId>database</artifactId>
      <version>0.1.0-SNAPSHOT</version>
   </dependency>
```

use `GraphDatabase.driver()` to connect local PandaDB, just like using neo4j:

```
  val builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbDir);
  ...
  val db = builder.newGraphDatabase();
  val tx = db.beginTx();
  ...
```
An alternative way is to use object `PandaDB`:

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

If you are used to `CypherService`, you may try the method `LocalGraphService.connect()`:
```
   val db = PandaDB.openDatabase(new File("./testdb"), new File("./neo4j.conf"));
   val conn = LocalGraphService.connect(db);
   //a non-blob
    val (node, name, age) = conn.querySingleObject("match (n) where n.name='bob' return n, n.name, n.age", (result: Record) => {
      (result.get("n").asNode(), result.get("n.name").asString(), result.get("n.age").asInt())
    });
```
`LocalGraphService.connect()` returns a `CypherService` too, just like that of `RemotePandaServer.connect()`.

more example code, see https://github.com/grapheco/pandadb-v0.1/blob/master/itest/src/test/scala/CypherServiceTest.scala

## 5. TODO

* batch import (spi: org.neo4j.unsafe.impl.batchimport.BatchImporterFactory)
* blob group

##  6. <a name='Licensing'></a>Licensing
PandaDB v0.1 is an open source product licensed under GPLv3.
