# Reproduce CA-16143
## Build the cluster
- Cassandra version : 4.0-beta4, 4.0
- Build the cluster as the guideline in cassandra.
## Step2:Replace SchedulerServiceImpl in Sieve server
```
# In Sieve server
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/
```
## Step3:Run test
```
# In Sieve server (failslow4)
./startEngine.sh cassandra 1
# In failslow1,2,3
./workloadDriver_cassandra.sh 1 1|2|3
```
## Fault point
AsyncMessageOutputPlus#doFlush