#!/bin/bash

sysName=$1
testNum=$2

failslowJar="/failslow/target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar"
mvn clean
mvn package
# run engine
java -cp ".:$failslowJar" failslow.Engine $sysName $testNum
