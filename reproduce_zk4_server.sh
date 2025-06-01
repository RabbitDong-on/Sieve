#!/bin/bash

sysName=$1
testNum=$2

mv src/main/java/scheduler/SchedulerServiceImpl.java .
cp Reproduce/DetectedBug/ZK-4/SchedulerServiceImpl.java src/main/java/scheduler/

failslowJar="/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar"
mvn clean
mvn package
# run engine
java -cp ".:$failslowJar" failslow.Engine $sysName $testNum

rm -rf src/main/java/scheduler/SchedulerServiceImpl.java
mv SchedulerServiceImpl.java src/main/java/scheduler/


