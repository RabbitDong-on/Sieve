# Reproduce HBase-14498
## Build the cluster
- HBase version : 3.0.0-alpha-1, 1.5.0, 2.0.0, 2.2.0
- Build the cluster as the guideline in HBase.
## Step2:Replace SchedulerServiceImpl in Sieve server
```
# In Sieve server
rm -rf /src/main/java/scheduler/SchedulerServiceImpl.java
cp SchedulerServiceImpl.java /src/main/java/scheduler/
```
## Step3:Run test
```
# In Sieve server (failslow4)
./startEngine.sh hbase 1
# In failslow1,2,3
./workloadDriver_hbase.sh 1 1|2|3
```
## Fault point
HRegion#init