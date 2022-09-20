# Start Instruction
> This is the instruction for starting PandaDB under single mode and distributed mode.
## Start single node PandaDB

#### (1) Download package

visit https://github.com/grapheco/pandadb-v0.2/releases to get pandadb-v0.2 binary distributions.

unpack `pandadb-server-<version>-unix.tar.gz` in your local directory, e.g. `/usr/local/`.

#### (2) Modify the configuration file

```
cd /usr/local/pandadb-server-<version>
vi conf/neo4j.conf
```

- modify costore related configurations refer to the following example:

 ```
  costore.enable=true
  # replace <es-host> and <es-port> with actual hostname and port
  costore.es.host=<es-host>
  costore.es.port=<es-port>
  costore.es.index=pandadb-costore
  costore.es.type=nodes
 ```

- modify HBase Blob Storage related configurations refer to the following example:

 ```
  # replace <zk-host:port> with actual hbase zookeeper quorum
  blob.storage.hbase.zookeeper.quorum=<zk-host:port>
  blob.storage.hbase.auto_create_table=true
  blob.storage.hbase.table=PANDADB_BLOB
 ```

- modify AIPM configurations refer to the following example:

 ```
   # replace <aipm-url> with actual AIPM URL
   aipm.http.host.url=<aipm-url>
 ```

#### (3) start

- modify configuration file to unenable `cn.pandadb.jraft.enabled`

```
  cn.pandadb.jraft.enabled=false
```

- start PandaDB server

```
  # start a PandaDB server silently
  cd /usr/local/pandadb-server-<version>
  bin/neo4j start
```



## Start multi-node PandaDB

(1) three copies of the PandaDB installation package in three different directories or on three machines.

(2) modify configuration file to enable `cn.pandadb.jraft.enabled` and set `cn.pandadb.jraft.server.peers` on all copies   refer to the following example:

```
cn.pandadb.jraft.enabled=true
cn.pandadb.jraft.server.peers=node1:8081,node2:8081,node3:8081
```

(3) set `cn.pandadb.jraft.server.id`

- modify configuration file on node1

```
cn.pandadb.jraft.server.id=node1:8081
```

- modify configuration file on node2

```
cn.pandadb.jraft.server.id=node2:8081
```

- modify configuration file on node3

```
cn.pandadb.jraft.server.id=node3:8081
```

(4) run below command on three nodes to start cluster

```
bin/neo4j start
```

## Start AIPM
