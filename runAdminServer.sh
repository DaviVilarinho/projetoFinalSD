#!/bin/bash
#

mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.AdminPortalServer -Dlog4j.rootLogger=DEBUG &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.AdminPortalServer -Dexec.args="25507" -Dlog4j.rootLogger=DEBUG &
