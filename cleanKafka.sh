#!/bin/bash

# ./kafka/bin/kafka-server-stop.sh
jps|grep "Kafka"|awk '{print $1}'|xargs kill -9

cd /apache-zookeeper-3.7.2-bin 
./bin/zkServer.sh stop
rm -rf data/version-2
rm -rf logs/*

cd /failslow/kafka
rm -rf logs/*
rm -rf /tmp/kafka-logs/*

cd /failslow
rm -rf system.log
rm -rf result.txt
rm -rf status.log