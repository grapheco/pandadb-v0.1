# PandaDB

## What is PandaDB?
PandaDB is a cloud native graph database. PandaDB aims to supports Hybrid Transactional and Analytical Processing (HTAP) workloads. It is Cypher compatible and features horizontal scalability, strong consistency, and high availability. PandaDB aims to provide a unified graph process interface for constructing and managing graphs on different key-value storage engines, such as RocksDB, HBase, and TiKV.

## Quick Start

### Building PandaDB

```bash
mvn clean compile install
```

### Running the Test
```bash
mvn clean install test
```

### Start a database instance
```bash
./bin/start 
```

### Interactive Shell
```bash
./bin/cypher
```

## Community

### Get connected
We provide multiple channels to connect you to the community of the PandaDB developers, users, and the general graph academic researchers:

* Our Slack channel
* Mail list

## Configuration
Please refer to the [Configuration Guide](docs/Configuration.md) in the online documentation for an overview on how to configure PandaDB.

## Contributing
Please review the [Contributing Guide](docs/conduct.md) for information on how to get started contributing to the project.