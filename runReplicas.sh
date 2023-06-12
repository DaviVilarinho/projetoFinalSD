#!/bin/bash

java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 0 p1 &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 0 p2 &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 0 p3 &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 1 p1 &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 1 p2 &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.distributedDatabase.RatisServer 1 p3 &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.AdminPortalServer &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.AdminPortalServer 25507 &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.OrderPortalServer &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.OrderPortalServer 60553