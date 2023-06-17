#!/bin/bash

mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.OrderPortalServer -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.OrderPortalServer -Dexec.args="60553" -Dlog4j.rootLogger=DEBUG   &
