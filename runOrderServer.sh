#!/bin/bash

java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.OrderPortalServer &
java -cp target/projetoFinalSD-1.0-SNAPSHOT-jar-with-dependencies.jar ufu.davigabriel.server.OrderPortalServer 60553 &
