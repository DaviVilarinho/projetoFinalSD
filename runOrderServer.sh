#!/bin/bash

java -Dlog4j.rootLogger=DEBUG -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.OrderPortalServer &
java -Dlog4j.rootLogger=DEBUG -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.OrderPortalServer 60553 &
