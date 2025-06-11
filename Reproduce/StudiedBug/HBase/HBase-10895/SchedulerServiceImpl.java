package scheduler;
import utils.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


import failslow.*;
import jsonutils.DelayPoint;
import jsonutils.MemAccess;
import jsonutils.TimeOutHandler;
/*
 * SchedulerServiceImpl
 * 
 */
import jsonutils.TimeOutPoint;

public class SchedulerServiceImpl extends UnicastRemoteObject implements SchedulerService{

    //trialdp
    public List<ScheduleInfo> delayPointList=new ArrayList<>();
    public List<DelayPoint> totalDP=new ArrayList<>();
    // delay
    public int count=0;
    public SchedulerServiceImpl() throws RemoteException{}
    // lock for singal|wait inject first delay
    public static ReentrantLock lock=new ReentrantLock();
    public static Condition aCondition=lock.newCondition();
    // lock for reorder memory accesses
    public static ReentrantLock reLock=new ReentrantLock();
    public static Condition reCondition=reLock.newCondition();

    @Override
    public synchronized ScheduleRes isInjectException(ScheduleInfo info) throws RemoteException{
        ScheduleRes res=new ScheduleRes();
        String className=info.className;
        String funcName=info.funcName;
        int lineNum=info.lineNum;
        Iterator<DelayPoint> dpIter=Engine.exceptionMap.iterator();
        while(dpIter.hasNext()){
            DelayPoint dp=dpIter.next();
            if(dp.className.equals(className)&&dp.funcName.equals(funcName)&&dp.lineNum==lineNum){
                int size=dp.exception.size();
                if(dp.index<size){
                    String exceptionName=dp.exception.get(dp.index%size);
                    dp.index++;
                    res.isInject=true;
                    res.exceptionName=exceptionName;
                }else{
                    res.isInject=false;
                }
            }
        }
        return res;
    }

    public void recordTotalDP(String className,String funcName,int lineNum){
        Iterator<DelayPoint> dIterator=totalDP.iterator();
        while(dIterator.hasNext()){
            DelayPoint dp=dIterator.next();
            if(dp.className.equals(className)&&dp.funcName.equals(funcName)&&dp.lineNum==lineNum){
                return;
            }
        }
        DelayPoint dp=new DelayPoint();
        dp.className=className;
        dp.funcName=funcName;
        dp.lineNum=lineNum;
        totalDP.add(dp);
    }
    

    /*
     * inject delay point in special pos
     * true: inject
     * false: cannot inject
     */
    public boolean filter(ScheduleInfo info){
        if(EngineDCT.isInjected||!EngineDCT.isStarted){
            return false;
        }
        // 
        Iterator<DelayPoint> dIter=totalDP.iterator();
        while(dIter.hasNext()){
            DelayPoint dp=dIter.next();
            if(dp.className.equals(info.className)&&
            dp.funcName.equals(info.funcName)&&
            dp.lineNum==info.lineNum
            ){
                return false;
            }
        }
        // System.out.println("SchedulerServiceImpl | filter true");
        recordTotalDP(info.className, info.funcName, info.lineNum);
        return true;
    }

    /*
     * isInjectDelay
     * agentiddelay
     * 
     */
    @Override
    public synchronized ScheduleRes isInjectDelay(ScheduleInfo info) throws RemoteException{
        ScheduleRes res=new ScheduleRes();
        // System.out.println("SchedulerServiceImpl | rmi:isInjectDelay");
        // record total delay point
        recordTotalDP(info.className, info.funcName, info.lineNum);
        if(EngineDCT.isDCT){
            if(filter(info)){
                res.isInject=true;
                res.delayTime=30*1000;
                EngineDCT.isInjected=true;
                System.out.println("SchedulerServiceImpl | className:"+info.className);
                System.out.println("SchedulerServiceImpl | funcName:"+info.funcName);
                System.out.println("SchedulerServiceImpl | lineNum:"+info.lineNum);
                System.out.println("SchedulerServiceImpl | injectedNum:"+totalDP.size());
            }else{
                res.isInject=false;
                res.delayTime=0;
                // System.out.println("SchedulerServiceImpl | injection failed");
            }
            return res;
        }else if(checker(info)){
            if(!info.funcName.equals("sendRegionClose")){
                res.isInject=false;
                res.delayTime=0;
                return res;
            }
            res=schedule(info);
            if(res.delayTime<0){
                res.isInject=false;
                res.delayTime=0;
                return res;
            }
            res.delayTime=25000;
            // for concurrency bug
            EngineDC.curClzName=info.className;
            EngineDC.curFuncName=info.funcName;
            EngineDC.curLineNum=info.lineNum;

            Engine.className=info.className;
            Engine.funcName=info.funcName;
            Engine.lineNum=info.lineNum;
            Engine.timeout=res.delayTime;
            Engine.isInjected=true;
            Engine.injectNum++;
            count++;
            System.out.println("SchedulerServiceImpl | className:"+info.className);
            System.out.println("SchedulerServiceImpl | funcName:"+info.funcName);
            System.out.println("SchedulerServiceImpl | lineNum:"+info.lineNum);
            System.out.println("SchedulerServiceImpl | type:"+info.type);
            System.out.println("SchedulerServiceImpl | tId:"+info.tId);
            System.out.println("SchedulerServiceImpl | nodeId:"+info.nodeId);
            System.out.println("SchedulerServiceImpl | timeout:"+res.delayTime);
            System.out.println("SchedulerServiceImpl | scheduler:"+count);
            return res;
        }else {
            res.isInject=false;
            res.delayTime=0;
            return res;
        }
    }
    
    public void printDP(){
        System.out.println("SchedulerServiceImpl | dp size:"+delayPointList.size());
        Iterator<ScheduleInfo> iter=delayPointList.iterator();
        int count=0;
        while(iter.hasNext()){
            ScheduleInfo info=iter.next();
            System.out.println("SchedulerServiceImpl | className:"+info.className);
            System.out.println("SchedulerServiceImpl | funcName:"+info.funcName);
            System.out.println("SchedulerServiceImpl | lineNum:"+info.lineNum);
            System.out.println("SchedulerServiceImpl | type:"+info.type);
            System.out.println("SchedulerServiceImpl | tId:"+info.tId);
            System.out.println("SchedulerServiceImpl | inject time:"+info.steList.size());
            count+=info.steList.size();
        }
        System.out.println("SchedulerServiceImpl | total inject time:"+count);
        System.out.println("SchedulerServiceImpl | total executed DP:"+totalDP.size());
    }

    /*
     * schedule
     * delaypointinject delay
     */
    public ScheduleRes schedule(ScheduleInfo info){
        ScheduleRes res=new ScheduleRes();
        res.isInject=true;
        // synchronize 10min
        if(info.type==1){
            System.out.println("SchedulerServiceImpl | synchronize");
            // res.delayTime=10*60*1000;
            res.delayTime=30000;
        }else if(info.type==2){ // timeout
            System.out.println("SchedulerServiceImpl | timeout");
            long delayTime=getTimeOut(info);
            // delaypoint
            // res.delayTime=3*delayTime/2
            res.delayTime=30000;
        }else{
            System.out.println("SchedulerServiceImpl | other");
            res.delayTime=0;
        }
        return res;
    }
    public long getTimeOut(ScheduleInfo info){
        for(Map.Entry<DelayPoint,List<TimeOutPoint>>entry:Engine.map.entrySet()){
            DelayPoint dp=entry.getKey();
            if(dp.className.contains(info.className)&&dp.funcName.contains(info.funcName)){
                if(dp.toVList.size()==0){
                    // System.out.println("SchedulerServiceImpl | no timeout value");
                }else{
                    if(dp.toVList.size()>dp.index){
                        long timeoutValue=dp.toVList.get(dp.index);
                        dp.index=dp.index+1;
                        return timeoutValue;
                    }else{
                        dp.index=0;
                    }
                }
            }
        }
        // iotimeout，timeout（dp）
        return -1;
    }
    /*
     * checker
     * infoinject delay
     * 
     */
    public boolean checker(ScheduleInfo curInfo){
        // 
        if(!Engine.isStarted){
            // 
            return false;
        }
        // trialinject delayinject delay
        if(Engine.isStarted&&Engine.isInjected){
            // System.out.println("inject once");
            return false;
        }
        // delay
        // if(Engine.isStarted&&Engine.injectNum>=2){
        //     return false;
        // }
        // 
        // ste，inject，ste
        Iterator<ScheduleInfo> iterScheduleInfo= delayPointList.iterator();
        while(iterScheduleInfo.hasNext()){
            ScheduleInfo info=iterScheduleInfo.next();
            // delay point
            // tidcheck？
            if(info.className.equals(curInfo.className)&&info.funcName.equals(curInfo.funcName)&&info.lineNum==curInfo.lineNum){
                // ste
                Iterator<StackTraceElement[]> steIter=info.steList.iterator();
                while(steIter.hasNext()){
                    StackTraceElement[] ste=steIter.next();
                    int len=ste.length;
                    int curLen=curInfo.lastSTE.length;
                    if(len==curLen){
                        boolean isSame=true;
                        for(int i=0;i<len;i++){
                            StackTraceElement element1=ste[i];
                            StackTraceElement curEle=curInfo.lastSTE[i];
                            if(!checkStackTraceElement(element1,curEle)){
                                isSame=false;
                                break;
                            }
                        }
                        if(isSame){
                            // System.out.println("SchedulerServiceImpl | isSame:true");
                            return false;
                        }
                    }
                }
                // new ste
                info.steList.add(curInfo.lastSTE);
                return true;
            }
        }
        // delay pointinject delay
        record(curInfo);
        return true;
    }
    /*
     * checkStackTraceElement
     * stacktraceelement
     * true
     * false
     */
    public boolean checkStackTraceElement(StackTraceElement ele1,StackTraceElement ele2){
        String className1=ele1.getClassName();
        String funcName1=ele1.getMethodName();
        int lineNum1=ele1.getLineNumber();
        String className2=ele2.getClassName();
        String funcName2=ele2.getMethodName();
        int lineNum2=ele2.getLineNumber();
        if(className1.equals(className2)&&funcName1.equals(funcName2)&&lineNum1==lineNum2){
            return true;
        }
        return false;
    }
    /*
     * record
     * trialdelay point
     */
    public boolean record(ScheduleInfo info){
        info.steList.add(info.lastSTE);
        int size=delayPointList.size();
        info.seqNum=size+1;
        delayPointList.add(info);
        return true;
    }
    @Override
    public void isGoingOn(int id) throws RemoteException{
        Engine.isFinish[id-1]=true;
    } 
    @Override
    public void startTrial(int id) throws RemoteException{
        Engine.isReport[id-1]=true;
        // report
        while((!(Engine.isReport[0]&&Engine.isReport[1]&&Engine.isReport[2]))){}
        // 
        // if(!EngineDCT.isDCT){
        //     while(!Engine.isStarted){}
        // }
    }
    @Override
    public void endTrial(int id) throws RemoteException{
        Engine.isReport[id-1]=false;
        while((Engine.isReport[0]||Engine.isReport[1]||Engine.isReport[2])){}
        // 
        // if(!EngineDCT.isDCT){
        //     while(Engine.isStarted){}
        // }
    }
    @Override
    public void isRestart(int id) throws RemoteException{
        Engine.isRestart[id-1]=true;
        while(!(Engine.isRestart[0]&&Engine.isRestart[1]&&Engine.isRestart[2])){}
    }
    @Override
    public void isCheck(int id) throws RemoteException{
        Engine.isCheck[id-1]=true;
        while(!(Engine.isCheck[0]&&Engine.isCheck[1]&&Engine.isCheck[2])){}
    }
    
    
    /*
     * report timeout value
     * ms
     * nanos
     */
    @Override
    public void reportConnect(int time,String toClassName,String toFuncName)throws RemoteException{
        // if(time==0){
        //     System.out.println("Connect time:"+time);
        // }
        storeTimeOutValue(time, toClassName, toFuncName);
    }
    @Override
    public void reportWait(long time,String toClassName,String toFuncName)throws RemoteException{
        // if(time==0){
        //     System.out.println("Wait time:"+time);
        // }
        storeTimeOutValue(time, toClassName, toFuncName);
    }
    @Override
    public long reportWait(long time,int nanos,String toClassName,String toFuncName)throws RemoteException{
        // if(time==0){
        //     System.out.println("Wait time:"+time+" nanos:"+nanos);
        // }
        if(nanos>500000){
            time++;
        }
        storeTimeOutValue(time, toClassName, toFuncName);
        return time;
    }
    @Override
    public void reportAwait(long time,String toClassName,String toFuncName)throws RemoteException{
        // if(time==0){
        //     System.out.println("Await time:"+time);
        // }
        storeTimeOutValue(TimeUnit.NANOSECONDS.toMillis(time), toClassName, toFuncName);
    }
    @Override
    public long reportAwait(long time,java.util.concurrent.TimeUnit arg,String toClassName,String toFuncName)throws RemoteException{
        // if(time==0){
        //     System.out.println("Await time:"+time+" unit:"+arg);
        // }
        storeTimeOutValue(arg.toMillis(time), toClassName, toFuncName);
        return time;
    }
    @Override
    public long reportTryAcquire(long time,java.util.concurrent.TimeUnit arg,String toClassName,String toFuncName)throws RemoteException{
        // if(time==0){
        //     System.out.println("TryAcquire time:"+time+" unit:"+arg);
        // }
        storeTimeOutValue(arg.toMillis(time), toClassName, toFuncName);
        return time;
    }
    @Override
    public void reportJoin(long time,String toClassName,String toFuncName)throws RemoteException{
        // if(time==0){
        //     System.out.println("Join time:"+time);
        // }
        storeTimeOutValue(time, toClassName, toFuncName);
    }
    @Override
    public long reportJoin(long time,int nanos,String toClassName,String toFuncName)throws RemoteException{
        // if(time==0){
        //     System.out.println("Join time:"+time+" nanos:"+nanos);
        // }
        if(nanos>500000){
            time++;
        }
        storeTimeOutValue(time, toClassName, toFuncName);
        return time;
    }
    @Override
    public long reportAddSub(long time,String toClassName,String toFuncName)throws RemoteException{
        storeTimeOutValue(time, toClassName, toFuncName);
        // if(time==0){
        //     System.out.println("AddSub:"+time);
        // }
        return time;
    }
    @Override
    public long reportCMP(long time,String toClassName,String toFuncName)throws RemoteException{
        storeTimeOutValue(time, toClassName, toFuncName);
        // if(time==0){
        //     System.out.println("CMP:"+time);
        // }
        return time;
    }
    @Override
    public void reportCMP(int time,String toClassName,String toFuncName)throws RemoteException{
        storeTimeOutValue((long)time, toClassName, toFuncName);
        // if(time==0){
        //     System.out.println("CMP:"+time);
        // }
    }
    public void storeTimeOutValue(long time,String toClassName,String toFuncName){
        for(Map.Entry<DelayPoint,List<TimeOutPoint>>entry:Engine.map.entrySet()){
            DelayPoint dp=entry.getKey();
            List<TimeOutPoint> toPList=entry.getValue();
            Iterator<TimeOutPoint> topIter=toPList.iterator();
            while(topIter.hasNext()){
                TimeOutPoint toP=topIter.next();
                if(toP.toClassName.contains(toClassName)&&toP.toFuncName.contains(toFuncName)){
                    // if(time==0){
                    //     System.out.println("SchedulerServiceImpl | toClassName:"+toClassName+" toFuncName:"+toFuncName+" time:"+time);
                    // }
                    if(!toP.toVList.contains(time)){
                        toP.toVList.add(time);
                        dp.toVList.add(time);
                    }
                }
            }
        }
        Iterator<TimeOutPoint> iter=Engine.toPList.iterator();
        while(iter.hasNext()){
            TimeOutPoint toP=iter.next();
            if(toP.toClassName.contains(toClassName)&&toP.toFuncName.contains(toFuncName)){
                // toP.timeoutValue=time;
                if(!toP.toVList.contains(time)){
                    toP.toVList.add(time);
                }
            }
        }
    }

    @Override
    public void reportTimeOutHandler(String className,String funcName,int lineNum)throws RemoteException{
        TimeOutHandler timeoutHandler=new TimeOutHandler();
        timeoutHandler.className=className;
        timeoutHandler.funcName=funcName;
        timeoutHandler.lineNum=lineNum;
        if(!EngineDC.timeoutHandlerInfo.timeoutHandlerList.contains(timeoutHandler)){
            EngineDC.timeoutHandlerInfo.timeoutHandlerList.add(timeoutHandler);
        }

    }
    
    
    @Override
    public  void reportWriteAccess(String className,String fieldName,String funcName,long lineNum, int hashCode,long tId,int nodeId)throws RemoteException{
        // System.out.println("SchedulerServiceImpl | reportWriteAccess");
        if(!EngineDC.isOver){
            MemAccess memAccess=new MemAccess();
            memAccess.className=className;
            memAccess.fieldName=fieldName;
            memAccess.hashCode=hashCode;
            memAccess.tId=tId;
            memAccess.nodeId=nodeId;
            memAccess.type=1;
            memAccess.funcName=funcName;
            memAccess.lineNum=(int)lineNum;
            EngineDC.lock.lock();
            if(EngineDC.isCollect){
                if(!EngineDC.memAccesses.memAccesses.contains(memAccess)){
                    EngineDC.memAccesses.memAccesses.add(memAccess);
                    EngineDC.totalCount++;
                }
            }else{
                if(!EngineDC.memAccesses.memAccesses.contains(memAccess)){
                    EngineDC.additionalMA.memAccesses.add(memAccess);
                    EngineDC.totalCount++;
                }
            }
            EngineDC.lock.unlock();
        }
    }
    /*
     * 
     */
    @Override
    public void reportReadAccess(String className,String fieldName,String funcName,long lineNum, int hashCode,long tId,int nodeId)throws RemoteException{
        // System.out.println("SchedulerServiceImpl | reportReadAccess");
        if(!EngineDC.isOver){
            MemAccess memAccess=new MemAccess();
            memAccess.className=className;
            memAccess.fieldName=fieldName;
            memAccess.hashCode=hashCode;
            memAccess.tId=tId;
            memAccess.nodeId=nodeId;
            memAccess.type=2;
            memAccess.funcName=funcName;
            memAccess.lineNum=(int)lineNum;
            EngineDC.lock.lock();
            if(EngineDC.isCollect){
                if(!EngineDC.memAccesses.memAccesses.contains(memAccess)){
                    EngineDC.memAccesses.memAccesses.add(memAccess);
                    EngineDC.totalCount++;
                }
            }else{
                if(!EngineDC.memAccesses.memAccesses.contains(memAccess)){
                    EngineDC.additionalMA.memAccesses.add(memAccess);
                    EngineDC.totalCount++;
                }
            }
            EngineDC.lock.unlock();
        }
    }
    @Override
    public void reportSleep(long time)throws RemoteException{
        try{
            // Thread.sleep(time);
            lock.lock();
            aCondition.await(time,TimeUnit.MILLISECONDS);
            lock.unlock();
        }catch(InterruptedException ie){
            ie.printStackTrace();
        }
    }
    public static void signal(){
        lock.lock();
        aCondition.signal();
        lock.unlock();
    }

    @Override
    public void Await(long seq)throws RemoteException{
        if(!EngineDCT.isInjected){
            return ;
        }
        if(EngineDCT.testedIDs.contains((int)seq)){
            return ;
        }
        if(EngineDCT.noId!=-1){
            if(EngineDCT.isTesting&&EngineDCT.noId==(int)seq){

            }else{
                return ;
            }
        }
        reLock.lock();
        if(!EngineDCT.isAwait){
            try{
                System.out.println("SchedulerServiceImpl | Await:"+seq);
                EngineDCT.isAwait=true;
                EngineDCT.noId=(int)seq;
                reCondition.await();
            }catch(InterruptedException ie){
                ie.printStackTrace();
            }
        }
        reLock.unlock();
    }

    @Override
    public void Signal(long seq)throws RemoteException{
        if(!EngineDCT.isInjected){
            return ;
        }
        if(!EngineDCT.isAwait){
            return ;
        }
        // System.out.println("SchedulerServiceImpl | Signal:"+seq);
        if(!EngineDCT.toIDs.contains((int)seq)){
            return ;
        }
        reLock.lock();
        if(EngineDCT.isAwait&&EngineDCT.toIDs.contains((int)seq)){
            System.out.println("SchedulerServiceImpl | Signal:"+seq);
            List<Integer> tgtList=EngineDCT.testedConflictPairList.get(EngineDCT.noId);
            if(tgtList==null){
                List<Integer> toList=new ArrayList<>();
                toList.add((int)seq);
                EngineDCT.testedConflictPairList.put(EngineDCT.noId, toList);
            }else{
                if(!tgtList.contains((int)seq)){
                    tgtList.add((int)seq);
                }
            }
            EngineDCT.toIDs.remove(EngineDCT.toIDs.indexOf((int)seq));
            if(EngineDCT.toIDs.size()==0){
                if(!EngineDCT.testedIDs.contains(EngineDCT.noId)){
                    EngineDCT.testedIDs.add(EngineDCT.noId);
                }
                EngineDCT.isTesting=false;
                EngineDCT.noId=-1;
            }
            reCondition.signalAll();
            EngineDCT.isTesting=true;
            EngineDCT.isAwait=false;
        }
        reLock.unlock();
    }
    @Override
    public void reportNonBlockingStartTime(ScheduleInfo info,long startTime)throws RemoteException{
        if(info.nodeId!=EngineNB.curNodeId){
            return ;
        }
        for(Map.Entry<DelayPoint,List<Long>>entry:EngineNB.dp2ET.entrySet()){
            DelayPoint dp=entry.getKey();
            if(dp.className.contains(info.className)&&
            dp.funcName.contains(info.funcName)&&
            dp.lineNum==info.lineNum){
                List<Long> timeList=entry.getValue();
                if(timeList.size()==0){
                    long st=System.currentTimeMillis();
                    timeList.add(st);
                }else{
                    // System.out.println("----------------now startTime:"+startTime);
                }
            }
        }
    }
    @Override
    public void reportNonBlockingEndTime(ScheduleInfo info,long endTime)throws RemoteException{
        if(info.nodeId!=EngineNB.curNodeId){
            return ;
        }
        for(Map.Entry<DelayPoint,List<Long>>entry:EngineNB.dp2ET.entrySet()){
            DelayPoint dp=entry.getKey();
            if(dp.className.contains(info.className)&&
            dp.funcName.contains(info.funcName)&&
            dp.lineNum==info.lineNum){
                List<Long> timeList=entry.getValue();
                if(timeList.size()==1){
                    long et=System.currentTimeMillis();
                    timeList.add(et);
                }else if(timeList.size()==2){
                    // System.out.println("----------------startTime:"+timeList.get(0)+" endTime:"+timeList.get(1));
                    // System.out.println("----------------now endTime:"+endTime);
                }
            }
        }
    }
    
}
