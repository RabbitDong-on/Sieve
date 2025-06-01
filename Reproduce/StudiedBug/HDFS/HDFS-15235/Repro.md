# Reproduce HDFS-15235
## Build the cluster
- HDFS version : 3.3.0
- Build the cluster as the guideline in HDFS.
## Step2:Replace SchedulerServiceImpl in Sieve server
```
# In Sieve server
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/
```
## Step3:Run test
```
# In Sieve server (failslow4)
./startEngine.sh hdfs 1
# In failslow1,2,3
./workloadDriver_hdfs.sh 1 1|2|3
(hdfs haadmin -failover NN1 NN2)
```
## Fault point
Sender#send