# Reproduce HBase-26256|27520
## Build the cluster
- HBase version : 2.4.2
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
HRegion#initializeRegionInternals