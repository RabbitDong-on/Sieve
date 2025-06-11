# Reproduce ZK-2219|2886
## Build the cluster
- Zookeeper version : 3.4.10, 3.5.3, 3.6.0
- Build the cluster as the guideline in zookeeper.
## Step2:Replace SchedulerServiceImpl in Sieve server
```
mv workload.sh /target_zoo_dir/bin
# In Sieve server
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/
```
## Step3:Run test
```
# In Sieve server (failslow4)
./startEngine.sh zoo 1
# In failslow1,2,3
./workloadDriver_zoo.sh 1 1|2|3
```
## Fault point
NIOServerCnxn{doIO}