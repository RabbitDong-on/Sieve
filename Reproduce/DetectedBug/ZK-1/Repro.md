# Reproduce ZK-1
## Step1:Replace SchedulerServiceImpl in Sieve server
```
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/SchedulerServiceImpl.java
```
## Step2:Run test
```
# In Sieve server (failslow4)
./startEngine.sh zoo 3
# In failslow1,2,3
./workloadDriver_zoo.sh 3 1|2|3
```
## Result
The follower spends more than 30s joining the cluster.
This bug needs multiple test runs. This may be a concurrency bug.
The system logs are attached.

## Fault point
sealStream