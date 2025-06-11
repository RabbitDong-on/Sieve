#!/bin/bash

sysName=$1
testOption=$2
nodeId=$3

# analysis point
failslowJar="./target/failslow-1.0-SNAPSHOT-jar-with-dependencies.jar"
java -cp ".:$failslowJar" failslow.Framework $sysName $nodeId $testOption
# java -cp ".:$failslowJar" faultpointanalysis.DelayParser $sysName
# inject delay
# java -cp ".:$failslowJar" faultinjection.DelayInjection $sysName
#inject exception
# java -cp ".:$failslowJar" faultinjection.ExceptionInjection $sysName
# trace timeout value
# java -cp ".:$failslowJar" faultpointanalysis.TraceTimeOut $sysName