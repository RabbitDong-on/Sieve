#!/bin/bash

id=$1

./bin/zkServer.sh stop
rm -rf *.log
rm -rf *.txt
rm -rf logs/*
rm -rf data/version-2
./workloadDriver_zoo.sh 1000 $id