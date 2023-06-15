#!/bin/bash

java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 0 p1 -Dlog4j.rootLogger=DEBUG   &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 0 p2 -Dlog4j.rootLogger=DEBUG   &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 0 p3 -Dlog4j.rootLogger=DEBUG   &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 1 p1 -Dlog4j.rootLogger=DEBUG   &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 1 p2 -Dlog4j.rootLogger=DEBUG   &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 1 p3 -Dlog4j.rootLogger=DEBUG  &
