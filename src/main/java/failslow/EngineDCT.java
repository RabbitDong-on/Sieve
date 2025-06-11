package failslow;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import jsonutils.ConflictPair;
import jsonutils.DP2ConflictPair;
import jsonutils.DP2ConflictPairs;
import jsonutils.JsonUtil;
import jsonutils.MemAccess;
import scheduler.SchedulerServiceImpl;

public class EngineDCT {
    public static final int FS_ENGINE_PORT=2359;

    public static boolean isDCT=false;
    public volatile static String dpClzName;
    public volatile static String dpFuncName;
    public volatile static int dpLineNum;
    public volatile static int toId=-1;
    public volatile static int noId=-1;
    public volatile static List<Integer> toIDs;

    public volatile static Map<Integer,List<Integer>> testedConflictPairList=new LinkedHashMap<>();

    // inject delay once for each conflict pair
    public volatile static boolean isInjected=false;
    public volatile static boolean isStarted=false;

    public volatile static boolean isAwait=false;
    public volatile static boolean isSignal=false;
    public volatile static boolean isFinish=false;
    public volatile static boolean isTesting=false;
    public static int count=0;

    // record tested id
    public static List<Integer>testedIDs=new ArrayList<>();

    public static void main(String[] args){
        if(args.length!=1){
            System.out.println("EngineDCT | please specify system name!");
            System.exit(0);
        }
        // start DCT
        isDCT=true;
        String sysName=args[0];
        Thread thread1=new Thread(new Runnable(){
            @Override
            public void run() {
                int testNum=0;
                JsonUtil jsonUtil=new JsonUtil();
                String path=System.getProperty("user.dir");
                String fileName=sysName+"DP2ConflictPairs";
                String context=jsonUtil.readJson(path,fileName);
                DP2ConflictPairs dp2ConflictPairs=JSON.parseObject(context,DP2ConflictPairs.class);
                Iterator<DP2ConflictPair> dp2CIter=dp2ConflictPairs.dp2ConflictedPairs.iterator();
                List<ConflictPair> conflictPairList=new ArrayList<>();
                while(dp2CIter.hasNext()){
                    DP2ConflictPair dPair=dp2CIter.next();
                    dpClzName=dPair.className;
                    dpFuncName=dPair.funcName;
                    dpLineNum=dPair.lineNum;
                    List<ConflictPair> conflictPairs=dPair.conflictPairs;
                    Iterator<ConflictPair> cPIter=conflictPairs.iterator();
                    while(cPIter.hasNext()){
                        ConflictPair conflictPair=cPIter.next();
                        if(!conflictPairList.contains(conflictPair)){
                            conflictPairList.add(conflictPair);
                        }
                        MemAccess toMA=conflictPair.toMemAccess;
                        MemAccess noMA=conflictPair.normalMemAccess;
                        if(toMA.className.equals(noMA.className)&&
                        toMA.funcName.equals(noMA.funcName)&&
                        toMA.lineNum==noMA.lineNum){
                            System.out.println("EngineDCT | await("+conflictPair.noId+") mem signal("+conflictPair.toId+")");
                        }
                    }
                }
                System.out.println("EngineDCT | conflictPairList:"+conflictPairList.size());
                // based on waitID, determine which signalID is needed
                isStarted=true;
                while(true){
                    // control delay point injection
                    // start to injected
                    isInjected=false;
                    System.out.println("EngineDCT | start Injection");
                    while(true){
                        // for this delay point, no conflict pair to test
                        boolean isOver=false;
                        while(true){
                            System.out.println("EngineDCT | here? noId:"+noId);
                            if(noId!=-1){
                                break;
                            }
                            int count=30;
                            while(count-->0){
                                try{
                                    if(noId!=-1){
                                        break;
                                    }
                                    Thread.sleep(1000);
                                }catch(InterruptedException ie){
                                    ie.printStackTrace();
                                }
                            }
                            if(count==-1&&noId==-1){
                                //  no conflict pair need to test
                                isOver=true;
                                break;
                            }
                        }
                        if(isOver){
                            break;
                        }
                        toIDs=selectSignalID(conflictPairList);
                        testNum++;
                        System.out.println("EngineDCT | testNum:"+testNum);
                        System.out.println("EngineDCT | noId:"+noId+" toIDs:"+toIDs);
                        while(true){
                            try{
                                int size=toIDs.size();
                                int count=30;
                                while(count-->0){
                                    Thread.sleep(1000);
                                    if((toIDs.size()-size)!=0||toIDs.size()==0){
                                        break;
                                    }
                                }
                                if(toIDs.size()==0){
                                    break;
                                }
                                if((toIDs.size()-size)==0){
                                    SchedulerServiceImpl.reLock.lock();
                                    if(!testedIDs.contains(noId)){
                                        testedIDs.add(noId);
                                    }
                                    noId=-1;
                                    SchedulerServiceImpl.reCondition.signalAll();
                                    EngineDCT.isAwait=false;
                                    EngineDCT.isTesting=false;
                                    SchedulerServiceImpl.reLock.unlock();
                                    break;
                                }
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                            if(toIDs.size()==0){
                                break;
                            }
                        }
                        System.out.println("EngineDCT | testedIDs:"+testedIDs);
                    }
                    // clear testedIDs
                    testedIDs.removeAll(testedIDs);
                    // cancel previous delay effect
                    SchedulerServiceImpl.signal();
                    System.out.println("EngineDCT | end injection");
                    if(isFinish){
                        break;
                    }
                }
                int count=0;
                for(Map.Entry<Integer,List<Integer>>entry:testedConflictPairList.entrySet()){
                    List<Integer> tmpList=entry.getValue();
                    count+=tmpList.size();
                    System.out.println("EngineDCT | noId:"+entry.getKey()+"-> toIDs:"+tmpList);
                }
                System.out.println("EngineDCT | total testedConflictedPairs:"+count);
            }
        });
        thread1.start();
        try{
            SchedulerServiceImpl schedulerService=new SchedulerServiceImpl();
            Registry registry=LocateRegistry.createRegistry(FS_ENGINE_PORT);
            Naming.rebind("rmi://localhost:2359/failslow",schedulerService);
            System.out.println("EngineDCT | start server, port is 2359");
            // check hang
            // <=30s
            boolean isGood=false;
            while(true){
                if(count>100000){
                    break;
                }
                Thread.sleep(3000);
                count++;
                if(Engine.isFinish[0]&&Engine.isFinish[1]&&Engine.isFinish[2]){
                    System.out.println("EngineDCT | Finish tests");
                    isFinish=true;
                    isGood=true;
                    break;
                }
            }
            thread1.join();

            System.out.println("EngineDCT | injection number:"+schedulerService.totalDP.size());

            // add end code
            registry.unbind("failslow");
            UnicastRemoteObject.unexportObject(registry, true);
            if(!isGood){
                if(Engine.isFinish[0]){
                    System.out.println("EngineDCT | client 1 finished!");
                }
                if(Engine.isFinish[1]){
                    System.out.println("EngineDCT | client 2 finished!");
                }
                if(Engine.isFinish[2]){
                    System.out.println("EngineDCT | client 3 finished!");
                }
                System.out.println("EngineDCT | client hang!");
            }
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<Integer> selectSignalID(List<ConflictPair> conflictPairList){
        List<Integer> res=new ArrayList<>();
        Iterator<ConflictPair> cPIter=conflictPairList.iterator();
        while(cPIter.hasNext()){
            ConflictPair cP=cPIter.next();
            if(cP.noId==noId){
                if(!res.contains(cP.toId)){
                    res.add(cP.toId);
                }
            }
        }
        return res;
    }
}
