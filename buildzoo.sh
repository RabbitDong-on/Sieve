#!/bin/bash

option=$1
testOption=$2
function buildzoo {
# download zookeeper
# git clone https://github.com/RabbitDong-on/zookeeper.git
# package zookeeper
cd zookeeper
mvn clean
# mvn clean install -DskipTests 
mvn compile
cd ..
# build failslow
mvn clean
mvn package
cat myid|xargs ./instrument.sh zoo $testOption
cd zookeeper
mvn package -Dmaven.test.skip=true -Dmaven.main.skip=true
mvn install -Dmaven.test.skip=true -Dmaven.main.skip=true
cp zookeeper-assembly/target/apache-zookeeper-3.10.0-SNAPSHOT-bin.tar.gz / 
cd / 
tar zxvf apache-zookeeper-3.10.0-SNAPSHOT-bin.tar.gz 
cd /failslow
cat myid|xargs ./genWorkload.sh zoo
cd /apache-zookeeper-3.10.0-SNAPSHOT-bin 
cp /failslow/zoo.cfg conf/zoo.cfg
mkdir data
cp /failslow/myid data/
cp /failslow/workload_seq.sh bin/
cp /failslow/workloadDriver_zoo.sh ./
cp /failslow/zkDCT.sh bin/
cp /failslow/reproduce_zk4_client.sh ./
cp /failslow/checker.sh ./
}

function mvjson2engine {
    docker cp failslow1:/failslow/zoo.json ./
    docker cp zoo.json failslow4:/failslow
    docker cp failslow1:/failslow/zooTOP2IO.json ./
    docker cp zooTOP2IO.json failslow4:/failslow
}

function mvjson2cluster {
    docker cp failslow4:/failslow/zooDP2ConflictPairs.json ./
    docker cp zooDP2ConflictPairs.json failslow1:/failslow
    docker cp zooDP2ConflictPairs.json failslow2:/failslow
    docker cp zooDP2ConflictPairs.json failslow3:/failslow
}


function main {
    if [ $option -eq 0 ]; then
        buildzoo
    elif [ $option -eq 1 ]; then
        mvjson2engine
    elif [ $option -eq 2 ]; then
        mvjson2cluster
    fi
}

main