#!/bin/bash

cd /
rm -rf apache*
cd failslow
mvn clean
cd zookeeper
mvn clean
