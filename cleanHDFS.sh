#!/bin/bash
# hdfs --daemon stop datanode
# hdfs --daemon stop journalnode
# hdfs --daemon stop zkfc

hdfs --daemon stop namenode
# hdfs -daemon stop datanode
./buildhdfs.sh 6
rm -rf result.txt
rm -rf status.log
rm -rf /hadoop/hadoop-3.3.6/logs/*
