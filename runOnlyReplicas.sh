#!/bin/bash

mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="0 p1" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="0 p2" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="0 p3" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="1 p1" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="1 p2" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="1 p3" -Dlog4j.rootLogger=DEBUG  &


