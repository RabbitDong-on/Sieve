package faultpointanalysis;

import jsonutils.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import soot.toolkits.graph.BriefUnitGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.jimple.toolkits.callgraph.TransitiveTargets;


/*
 * MethodCallerAnalysis
 */

public class MethodCallerAnalysis extends SceneTransformer{
    public String fileName;
    public String sysName;
    public Set<DelayPoint>dpSet;
    public List<SootMethod> inLockMethods;
    public static String packageName;
    public static String packageName_plus="xxxx";
    public String sysClassPath;
    public int nodeId;
    public int testOption;
    String path=System.getProperty("user.dir");
    
    public MethodCallerAnalysis(Set<DelayPoint> dpSet,String fileName,String sysName,List<SootMethod>inLockMethods,String sysClassPath,int nodeId,int testOption){
        this.dpSet=dpSet;
        this.fileName=fileName;
        this.sysName=sysName;
        this.inLockMethods=inLockMethods;
        this.sysClassPath=sysClassPath;
        this.nodeId=nodeId;
        this.testOption=testOption;
        // TODOEXTEND
        if(sysName.equals("zoo")){
            packageName="org.apache.zookeeper";
        }else if(sysName.equals("hdfs")){
            packageName="org.apache.hadoop.hdfs";
        }else if(sysName.contains("mapred")){
            packageName="org.apache.hadoop.mapred";
            packageName_plus="org.apache.hadoop.mapreduce";
        }else if(sysName.equals("cassandra")){
            packageName="org.apache.cassandra";
        }else if(sysName.equals("hbase")){
            packageName="org.apache.hadoop.hbase";
        }else if(sysName.contains("kafka")){
            packageName="kafka";
        }
    }
    /*
     * internalTransform
     */
    @Override
    protected void internalTransform(String s,Map<String,String>map){
        CHATransformer.v().transform();
        CallGraph cg=Scene.v().getCallGraph();
        Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap=new LinkedHashMap<>();
        SystemInfo sysInfo=new SystemInfo();
        JsonUtil jsonUtil=new JsonUtil();
        List<SootMethod>flag=new ArrayList<>();
        List<List<SootMethod>>callerChains=new ArrayList<>();
        // parseTimeout
        ParseTimeOut parseTimeOut=new ParseTimeOut(packageName,packageName_plus);
        // parseSync
        ParseSync parseSync=new ParseSync(packageName,packageName_plus,inLockMethods);
        Iterator<DelayPoint>dIterator=this.dpSet.iterator();
        List<SootMethod> analyzedIO=new ArrayList<>();
        // System.out.println(this.dpList.size());
        int count=this.dpSet.size();
        // int analysisNum=0;
        while(dIterator.hasNext()&&count>0){
            DelayPoint dp=dIterator.next();
            SootClass tgtSootClass=Scene.v().forceResolve(dp.className, SootClass.BODIES);
            SootMethod tgtMethod=tgtSootClass.getMethod(dp.funcName);
            int lineNum=dp.lineNum;
            // System.out.println("tgtMethod:"+tgtMethod+"-----flag:"+flag);
            if(!analyzedIO.contains(tgtMethod)){
                analyzedIO.add(tgtMethod);
            }
            if(!isAnalysised(csMap, tgtMethod)){
                // analysisNum++;
                flag.add(tgtMethod);
                // System.out.println(csMap.size());
                recur(tgtMethod,lineNum,csMap,cg,flag,callerChains);
                // System.out.print(csMap.size());
                flag.remove(tgtMethod);
                writeDelayPoint(csMap,tgtMethod,sysInfo);
                // flag.add(tgtMethod);
                // callerChainNum=ObtainCallerChain(csMap,tgtMethod,flag,callerChainNum,callerChains);
                // flag.remove(tgtMethod);
            }
            count--;
        }
        //parsetimeout
        Map<SootMethod,List<TimeOutPoint>> timeout2Info=parseTimeOut.parseTimeoutMethod();
        System.out.println("MethodCallerAnalysis | timeout2Info:"+timeout2Info.size());
        List<SootMethod> timeoutMethod=new ArrayList<>();
        Map<SootMethod,Map<SootMethod,List<SootMethod>>> gen2Scope=parseTimeOut.parseGeneralMethod(timeout2Info);
        System.out.println("MethodCallerAnalysis | gen2Scope:"+gen2Scope.size());
        Map<SootMethod,Map<SootMethod,Integer>> wat2NSR=parseTimeOut.parseWAT2NSRMethod(csMap, timeout2Info);
        System.out.println("MethodCallerAnalysis | wat2NSR:"+wat2NSR.size());
        Map<SootMethod,List<SootMethod>> join2Run=parseTimeOut.parseJoinMethod(csMap, timeout2Info, cg);
        List<SootMethod> timeoutSelfIO=parseTimeOut.parseTOSIO(this.dpSet);
        System.out.println("MethodCallerAnalysis | join2Run:"+join2Run.size());

        Map<SootMethod,List<SootMethod>>timeout2IO=new LinkedHashMap<>();
        Map<SootMethod,List<SootMethod>>wat2IO=parseTimeOut.ObtainWAT2IO(csMap,analyzedIO,wat2NSR,dpSet);
        System.out.println("MethodCallerAnalysis | wat2IO:"+wat2IO.size());
        Map<SootMethod,List<SootMethod>>join2IO=parseTimeOut.ObtainJoin2IO(csMap,analyzedIO,join2Run);
        System.out.println("MethodCallerAnalysis | join2IO:"+join2IO.size());
        Map<SootMethod,List<SootMethod>>gen2IO=parseTimeOut.ObtainGen2IO(csMap,analyzedIO,gen2Scope);
        System.out.println("MethodCallerAnalysis | gen2IO:"+gen2IO.size());
        timeout2IO=merge(wat2IO,join2IO,gen2IO);
        System.out.println("MethodCallerAnalysis | timeout2IO:"+timeout2IO.size());
        for(Map.Entry<SootMethod,List<SootMethod>>entry:timeout2IO.entrySet()){
            SootMethod toMethod=entry.getKey();
            if(!timeoutMethod.contains(toMethod)){
                timeoutMethod.add(toMethod);
            }
        }
        System.out.println("MethodCallerAnalysis | useful timeout:"+timeoutMethod.size());
        // parse TimeOut handler
        if(testOption==1){
            // parseTimeOutHandler.parseTOHandler(timeout2Info,timeoutMethod,nodeId);
            // parseTimeOutHandler.memAccessTracer(csMap,nodeId);
        }
        List<SootMethod> syncIOList=parseSync.parseSyncIO(analyzedIO, csMap);
        List<SootMethod> exSyncIO=parseSync.parseNSSyncIO(analyzedIO, csMap,dpSet);
        System.out.println("MethodCallerAnalysis | nsSyncIO:"+exSyncIO.size());
        List<SootMethod> joinSyncIO=parseSync.parseJoin(analyzedIO, csMap);
        System.out.println("MethodCallerAnalysis | joinSyncIO:"+joinSyncIO.size());
        List<SootMethod> syncSelfIOList=parseSync.parseSSIO(this.dpSet);
        System.out.println("MethodCallerAnalysis | selfSyncIO:"+syncSelfIOList.size());
        System.out.println("MethodCallerAnalysis | before syncIOList(within):"+syncIOList.size());
        Iterator<SootMethod> exsIter=exSyncIO.iterator();
        while(exsIter.hasNext()){
            SootMethod exsMethod=exsIter.next();
            if(!syncIOList.contains(exsMethod)){
                syncIOList.add(exsMethod);
            }
        }
        Iterator<SootMethod> joinIter=joinSyncIO.iterator();
        while(joinIter.hasNext()){
            SootMethod joinMethod=joinIter.next();
            if(!syncIOList.contains(joinMethod)){
                syncIOList.add(joinMethod);
            }
        }
        Iterator<SootMethod> selfIter=syncSelfIOList.iterator();
        while(selfIter.hasNext()){
            SootMethod selfMethod=selfIter.next();
            if(!syncIOList.contains(selfMethod)){
                syncIOList.add(selfMethod);
            }
        }
        System.out.println("MethodCallerAnalysis | after syncIOList:"+syncIOList.size());
        System.out.println("MethodCallerAnalysis | analysisNum:"+analyzedIO.size());
        System.out.println("MethodCallerAnalysis | csMap.size:"+csMap.size());

        // useful io
        List<SootMethod> timeoutIOList=new ArrayList<>();
        for(Map.Entry<SootMethod,List<SootMethod>>entry:timeout2IO.entrySet()){
            List<SootMethod> src=entry.getValue();
            Iterator<SootMethod> mIterator=src.iterator();
            while(mIterator.hasNext()){
                SootMethod method=mIterator.next();
                if(!timeoutIOList.contains(method)){
                    timeoutIOList.add(method);
                    if(syncIOList.contains(method)){
                        syncIOList.remove(method);
                    }
                }
            }
        }
        timeoutIOList.addAll(timeoutSelfIO);
        clean(timeoutIOList);
        clean(syncIOList);
        System.out.println("MethodCallerAnalysis | useful timeout io num:"+timeoutIOList.size());
        System.out.println("MethodCallerAnalysis | useful sync io num:"+syncIOList.size());
        System.out.println("MethodCallerAnalysis | useful io num:"+(timeoutIOList.size()+syncIOList.size()));
        writeTimeOut2IO(timeout2IO);
        writeTimeOutPoint(timeoutMethod, timeout2Info);
        writeTimeOutAndSyncIO(timeoutIOList, syncIOList, csMap);
        // String path=System.getProperty("user.dir");
        jsonUtil.writeJson(path,this.fileName,sysInfo);
    }
    public List<SootMethod> clean(List<SootMethod> tgtList){
        List<SootMethod> ex=new ArrayList<>();
        Iterator<SootMethod> tgtIter=tgtList.iterator();
        while(tgtIter.hasNext()){
            SootMethod tgtMethod=tgtIter.next();
            Iterator<SootMethod> exIter=tgtList.iterator();
            boolean isAdd=false;
            while(exIter.hasNext()){
                SootMethod exMethod=exIter.next();
                if(exMethod.getSignature().equals(tgtMethod.getSignature())){
                    if(!isAdd){
                        isAdd=true; 
                    }else{
                        ex.add(tgtMethod);
                        break;
                    }
                }
            }
        }
        System.out.println("MethodCallerAnalysis | clean ex:"+ex.size());
        return tgtList;
    }
    public void writeTimeOut2IO(Map<SootMethod,List<SootMethod>>timeout2IO){
        TOP2IO top2io=new TOP2IO(this.sysName);
        JsonUtil jsonUtil=new JsonUtil();
        for(Map.Entry<SootMethod,List<SootMethod>>entry:timeout2IO.entrySet()){
            SootMethod toMethod=entry.getKey();
            List<SootMethod> ioMethodList=entry.getValue();
            top2io.writeTOP2IO(toMethod,ioMethodList);
        }
        String fileName=sysName+"TOP2IO";
        // String path=System.getProperty("user.dir");
        jsonUtil.writeJson(path, fileName, top2io);
    }
    public void writeTimeOutPoint(List<SootMethod> timeoutMethod,Map<SootMethod,List<TimeOutPoint>> timeout2Info){
        SystemInfo systemInfo=new SystemInfo();
        JsonUtil jsonUtil=new JsonUtil();
        for(Map.Entry<SootMethod,List<TimeOutPoint>>entry:timeout2Info.entrySet()){
            SootMethod method=entry.getKey();
            if(timeoutMethod.contains(method)){
                List<TimeOutPoint> toPList=entry.getValue();
                Iterator<TimeOutPoint> toPIter=toPList.iterator();
                while(toPIter.hasNext()){
                    TimeOutPoint toP=toPIter.next();
                    systemInfo.writeTOPoint(sysName, toP.toClassName, toP.toFuncName, toP.toPos, toP.timeoutMethodType,toP.lr,toP.traceType);
                }
            }
        }
        String fileName=sysName+"TOPoint";
        // String path=System.getProperty("user.dir");
        jsonUtil.writeJson(path, fileName, systemInfo);
    }
    /*
     * writeTimeOutAndSyncIO
     */
    public void writeTimeOutAndSyncIO(List<SootMethod>timeoutIOList,List<SootMethod> syncIOList,Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap){
        SystemInfo systemInfo=new SystemInfo();
        JsonUtil jsonUtil=new JsonUtil();
        Iterator<DelayPoint>dIterator=this.dpSet.iterator();
        while(dIterator.hasNext()){
            DelayPoint dp=dIterator.next();
            Iterator<SootMethod> toIter=timeoutIOList.iterator();
            while(toIter.hasNext()){
                SootMethod timeoutIO=toIter.next();
                if(timeoutIO.getDeclaringClass().getName().equals(dp.className)&&timeoutIO.getSubSignature().equals(dp.funcName)){
                    systemInfo.writeSystemInfo(this.sysName, dp.className, dp.funcName, dp.lineNum, 2,null);
                }
            }
            Iterator<SootMethod> syncIter=syncIOList.iterator();
            while(syncIter.hasNext()){
                SootMethod syncIO=syncIter.next();
                if(syncIO.getDeclaringClass().getName().equals(dp.className)&&syncIO.getSubSignature().equals(dp.funcName)){
                    if(syncIO.getName().equals("storeOffsets")){
                        systemInfo.writeSystemInfo(this.sysName, dp.className, dp.funcName,345, 1,null);
                    }else{
                        systemInfo.writeSystemInfo(this.sysName, dp.className, dp.funcName,dp.lineNum, 1,null);
                    }
                    // systemInfo.writeSystemInfo(this.sysName, dp.className, dp.funcName,dp.lineNum, 1,null);
                }
            }
        }

        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            Map<SootMethod,Integer> tgtMap=entry.getKey();
            Map<SootMethod,Integer> srcMap=entry.getValue(); 
            for(Map.Entry<SootMethod,Integer>tgtEntry:tgtMap.entrySet()){
                SootMethod tgtMethod=tgtEntry.getKey();
                Iterator<SootMethod> toIter=timeoutIOList.iterator();
                while(toIter.hasNext()){
                    SootMethod timeoutIO=toIter.next();
                    if(timeoutIO.getDeclaringClass().getName().equals(tgtMethod.getDeclaringClass().getName())&&timeoutIO.getSubSignature().equals(tgtMethod.getSubSignature())){
                        if(srcMap.size()==0){
                            break;
                        }
                        for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                            SootMethod srcMethod=srcEntry.getKey();
                            int lineNum=srcEntry.getValue();
                            systemInfo.writeSystemInfo(this.sysName, srcMethod.getDeclaringClass().getName(), srcMethod.getSubSignature(), lineNum, 2,null);
                        }
                    }
                }
                Iterator<SootMethod> syncIter=syncIOList.iterator();
                while(syncIter.hasNext()){
                    SootMethod syncIO=syncIter.next();
                    if(syncIO.getDeclaringClass().getName().equals(tgtMethod.getDeclaringClass().getName())&&syncIO.getSubSignature().equals(tgtMethod.getSubSignature())){
                        if(srcMap.size()==0){
                            break;
                        }
                        for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                            SootMethod srcMethod=srcEntry.getKey();
                            int lineNum=srcEntry.getValue();
                            systemInfo.writeSystemInfo(this.sysName, srcMethod.getDeclaringClass().getName(), srcMethod.getSubSignature(), lineNum, 1,null);
                        }
                    }
                }
            }
        }

        String fileName=sysName+"TimeOutSync";
        System.out.println("MethodCallerAnalysis | "+fileName+":"+systemInfo.dpSet.size());
        // String path=System.getProperty("user.dir");
        jsonUtil.writeJson(path,fileName , systemInfo);
    }

    public Map<SootMethod,List<SootMethod>> merge(Map<SootMethod,List<SootMethod>>wat2IO,Map<SootMethod,List<SootMethod>>join2IO,Map<SootMethod,List<SootMethod>>gen2IO){
        Map<SootMethod,List<SootMethod>>res=new LinkedHashMap<>();
        List<SootMethod>timeoutMethodList=new ArrayList<>();
        for(Map.Entry<SootMethod,List<SootMethod>>entry:wat2IO.entrySet()){
            SootMethod method=entry.getKey();
            if(!timeoutMethodList.contains(method)){
                timeoutMethodList.add(method);
            }
        }
        for(Map.Entry<SootMethod,List<SootMethod>>entry:join2IO.entrySet()){
            SootMethod method=entry.getKey();
            if(!timeoutMethodList.contains(method)){
                timeoutMethodList.add(method);
            }
        }
        for(Map.Entry<SootMethod,List<SootMethod>>entry:gen2IO.entrySet()){
            SootMethod method=entry.getKey();
            if(!timeoutMethodList.contains(method)){
                timeoutMethodList.add(method);
            }
        }
        Iterator<SootMethod> timeoutIter=timeoutMethodList.iterator();
        while(timeoutIter.hasNext()){
            SootMethod timeoutMethod=timeoutIter.next();
            List<SootMethod> ioList=new ArrayList<>();
            List<SootMethod> ioList1=wat2IO.get(timeoutMethod);
            if(ioList1!=null){
                Iterator<SootMethod> ioIter=ioList1.iterator();
                while(ioIter.hasNext()){
                    SootMethod io=ioIter.next();
                    if(!ioList.contains(io)){
                        ioList.add(io);
                    }
                }
            }
            List<SootMethod> ioList2=join2IO.get(timeoutMethod);
            if(ioList2!=null){
                Iterator<SootMethod> ioIter=ioList2.iterator();
                while(ioIter.hasNext()){
                    SootMethod io=ioIter.next();
                    if(!ioList.contains(io)){
                        ioList.add(io);
                    }
                }
            }
            List<SootMethod> ioList3=gen2IO.get(timeoutMethod);
            if(ioList3!=null){
                Iterator<SootMethod> ioIter=ioList3.iterator();
                while(ioIter.hasNext()){
                    SootMethod io=ioIter.next();
                    if(!ioList.contains(io)){
                        ioList.add(io);
                    }
                }
            }
            res.put(timeoutMethod, ioList);
        }
        return res;
    }

    /*
     * ObtainCallerChain
     * 
     */
    public static long ObtainCallerChain(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,SootMethod tgtMethod,List<SootMethod> flag,long count,List<List<SootMethod>>callerChains){
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            Map<SootMethod,Integer> tgtMap=entry.getKey();
            Map<SootMethod,Integer> srcMap=entry.getValue(); 
            // System.out.println(tgtMap.size()+"=============================================="+srcMap.size());
            // System.out.println(entry);
            for(Map.Entry<SootMethod,Integer>tgtEntry:tgtMap.entrySet()){
                if(tgtEntry.getKey().equals(tgtMethod)){
                    if(srcMap.size()==0){
                        count++;
                        System.out.println("within for current callerChainCount:"+count+"  ---------  current chain len:"+flag.size());
                        boolean res=addCallerChain(flag,callerChains);
                        if(!res){
                            System.out.println("within for flag:"+flag+"---flag size:"+flag.size());
                        }
                        return count;
                    }
                    for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                        System.out.println("Location:"+tgtEntry.getValue()+" "+tgtEntry.getKey());
                        System.out.println("in CallSite:"+srcEntry.getValue()+"--invoked by-->");
                        System.out.println("Location:"+srcEntry.getKey().getJavaSourceStartLineNumber()+" "+srcEntry.getKey());
                        System.out.println("--------------------------------------------------------------");
                        if(!flag.contains(srcEntry.getKey())){
                            flag.add(srcEntry.getKey());
                            // System.out.println(flag);
                            long temp=ObtainCallerChain(csMap, srcEntry.getKey(),flag,count,callerChains);
                            count=temp;
                            flag.remove(srcEntry.getKey());
                            // return count;
                        }else{
                            System.out.println("flag.contain is effective!:"+flag);
                        }
                    }
                    System.out.println("error path:"+flag);
                    return count;
                }
            }
        }
        count++;
        System.out.println("outof for current callerChainCount:"+count+"  ---------  current chain len:"+flag.size()+"  -------  flag:"+flag);
        boolean res=addCallerChain(flag,callerChains);
        if(!res){
            System.out.println("outof for flag:"+flag+"---flag size:"+flag.size());
        }
        return count;
    }

    /*
     * ObtainCallerChain
     */
    public static long ObtainDependencyChain(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,SootMethod tgtMethod,List<SootMethod> flag,long count,List<List<SootMethod>>callerChains){
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            Map<SootMethod,Integer> tgtMap=entry.getKey();
            Map<SootMethod,Integer> srcMap=entry.getValue(); 
            for(Map.Entry<SootMethod,Integer>tgtEntry:tgtMap.entrySet()){
                if(tgtEntry.getKey().equals(tgtMethod)){
                    if(tgtEntry.getValue()==-1){
                        count++;
                        System.out.println("within for current callerChainCount:"+count+"  ---------  current chain len:"+flag.size());
                        boolean res=addCallerChain(flag,callerChains);
                        if(!res){
                            System.out.println("within for flag:"+flag+"---flag size:"+flag.size());
                        }
                        return count;
                    }
                    for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                        // System.out.println("Location:"+tgtEntry.getValue()+" "+tgtEntry.getKey());
                        // System.out.println("in CallSite:"+srcEntry.getValue()+"--invoked by-->");
                        // System.out.println("Location:"+srcEntry.getKey().getJavaSourceStartLineNumber()+" "+srcEntry.getKey());
                        // System.out.println("--------------------------------------------------------------");
                        if(!flag.contains(srcEntry.getKey())){
                            flag.add(srcEntry.getKey());
                            // System.out.println(flag);
                            long temp=ObtainCallerChain(csMap, srcEntry.getKey(),flag,count,callerChains);
                            count=temp;
                            flag.remove(srcEntry.getKey());
                            // return count;
                        }else{
                            System.out.println("flag.contain is effective!:"+flag);
                        }
                    }
                    System.out.println("error path:"+flag);
                    return count;
                }
            }
        }
        count++;
        System.out.println("outof for current callerChainCount:"+count+"  ---------  current chain len:"+flag.size()+"  -------  flag:"+flag);
        boolean res=addCallerChain(flag,callerChains);
        if(!res){
            System.out.println("outof for flag:"+flag+"---flag size:"+flag.size());
        }
        return count;
    }

    public static boolean addCallerChain(List<SootMethod> flag,List<List<SootMethod>>callerChains){
        boolean isExisted=false;
        Iterator<List<SootMethod>>iterator=callerChains.iterator();
        while(iterator.hasNext()){
            List<SootMethod> callerChain=iterator.next();
            if(callerChain.size()==flag.size()){
                Iterator<SootMethod> cIter=callerChain.iterator();
                Iterator<SootMethod> fIter=flag.iterator();
                boolean subFlag=true;
                while(cIter.hasNext()){
                    SootMethod cMethod=cIter.next();
                    SootMethod fMethod=fIter.next();
                    if(!cMethod.equals(fMethod)){
                        subFlag=false;
                        break;
                    }
                }
                if(subFlag){
                    isExisted=true;
                    break;
                }
            }
        }
        if(isExisted){
            return false;
        }
        //add caller chain
        List<SootMethod> temp=new ArrayList<>();
        Iterator<SootMethod> iter=flag.iterator();
        while(iter.hasNext()){
            temp.add(iter.next());
        }
        callerChains.add(temp);
        return true;
    }

    public static void recur(SootMethod tgtMethod,int tgtMethodLineNum,Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,CallGraph cg,List<SootMethod>flag,List<List<SootMethod>>callerChains){
        int tgtLineNum=tgtMethodLineNum;
        if(tgtLineNum==-1){
            System.out.println("MethodCallerAnalysis | unexpected -1");
            return ;
        }
        Iterator<MethodOrMethodContext> callSites=new Sources(cg.edgesInto(tgtMethod));
        if(!callSites.hasNext()){
            Map<SootMethod,Integer>tgtMap=new LinkedHashMap<>();
            tgtMap.put(tgtMethod, tgtLineNum);
            Map<SootMethod,Integer>srcMap=new LinkedHashMap<>();
            csMap.put(tgtMap, srcMap);
            // addCallerChain(flag, callerChains);
            return ;
        }
        Map<SootMethod,Integer>tgtMap=new LinkedHashMap<>();
        tgtMap.put(tgtMethod, tgtLineNum);
        Map<SootMethod,Integer>srcMap=new LinkedHashMap<>();
        while(callSites.hasNext()){
            SootMethod cs=(SootMethod)callSites.next();
            if(!isSystemMethod(cs)){
                continue;
            }
            Set<Integer>lineNumSet=getLineNum(cs,tgtMethod,cg);
            Iterator<Integer>lineIterator=lineNumSet.iterator();
            while(lineIterator.hasNext()){
                int lineNum=lineIterator.next();
                // if(tgtMethod.getName().equals("startZkServer")){
                //     System.out.println("src"+cs.getName()+" pos:"+lineNum);
                // }
                srcMap.put(cs, lineNum);
            }
        }
        csMap.put(tgtMap, srcMap);
        // System.out.println(srcMap.size()+" edges into: "+tgtMethod);
        for(Map.Entry<SootMethod,Integer>entry:srcMap.entrySet()){
            SootMethod nextTgtMethod=entry.getKey();

            int nextTgtLineNum=entry.getValue();
            if(!isAnalysised(csMap, nextTgtMethod)){
                // System.out.println("tgtMethod:"+tgtMethod.getName()+"---->"+"nextTgtMethod:"+nextTgtMethod.getName());
                flag.add(nextTgtMethod);
                // System.out.print(csMap.size());
                recur(nextTgtMethod,nextTgtLineNum,csMap, cg,flag,callerChains);
                // System.out.print(csMap.size());
                flag.remove(nextTgtMethod);
            }

        }
    }

    public static void pendCallerChain(List<SootMethod>flag,SootMethod method,List<List<SootMethod>>callerChains){
        Iterator<List<SootMethod>>iterator=callerChains.iterator();
        List<List<SootMethod>> tempcallerChains=new ArrayList<>();
        while(iterator.hasNext()){
            List<SootMethod> methodList=iterator.next();

            Iterator<SootMethod> mIterator=methodList.iterator();
            int index=0;
            List<SootMethod>temp=new ArrayList<>();
            while(mIterator.hasNext()){
                SootMethod sootMethod=mIterator.next();
                if(sootMethod.equals(method)){
                    temp.addAll(flag);
                    temp.addAll(methodList.subList(index, methodList.size()));
                    addCallerChain(temp, tempcallerChains);
                    break;
                }
                index++;
            }
        }
        callerChains.addAll(tempcallerChains);
    }

    public static boolean isAnalysised(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,SootMethod method){
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            for(Map.Entry<SootMethod,Integer>en:entry.getKey().entrySet()){
                if(en.getKey().equals(method)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSystemMethod(SootMethod method){
        if(method.getDeclaringClass().getPackageName().contains(packageName)||method.getDeclaringClass().getPackageName().contains(packageName_plus)){
            return true;
        }
        return false;
    }
    
    public static Set<Integer> getLineNum(SootMethod callerMethod,SootMethod tgtMethod,CallGraph cg){
        Set<Integer> lineNumSet=new HashSet<>();
        if(callerMethod.isConcrete()){
            JimpleBody body=(JimpleBody)callerMethod.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()&&stmt.getInvokeExpr().getMethod().getSubSignature().equals(tgtMethod.getSubSignature())){
                    // return stmt.getJavaSourceStartLineNumber();
                    if(stmt.getJavaSourceStartLineNumber()!=-1){
                        lineNumSet.add(stmt.getJavaSourceStartLineNumber());
                    }
                }
            }
            if(lineNumSet.size()!=0){
                return lineNumSet;
            }
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    SootMethod cs=stmt.getInvokeExpr().getMethod();
                    if(cs.getSignature().equals("<java.lang.Thread: void start()>")){
                        Iterator<MethodOrMethodContext> callees=new Targets(cg.edgesOutOf(callerMethod));
                        while(callees.hasNext()){
                            SootMethod callee=(SootMethod)callees.next();
                            if(callee.equals(tgtMethod)){
                                // System.out.println("callerMethod==="+callerMethod+"===caller==="+caller+"===callee==="+callee);
                                // return stmt.getJavaSourceStartLineNumber();
                                if(stmt.getJavaSourceStartLineNumber()!=-1){
                                    lineNumSet.add(stmt.getJavaSourceStartLineNumber());
                                }
                            }
                        }
                    }
                }
            }
        }
        return lineNumSet;
    }

    public void writeDelayPoint(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,SootMethod tgtMethod,SystemInfo sysInfo){
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            Map<SootMethod,Integer> tgtMap=entry.getKey();
            Map<SootMethod,Integer> srcMap=entry.getValue();
            assert tgtMap.size()==1;
            for(Map.Entry<SootMethod,Integer>tgtEntry:tgtMap.entrySet()){
                if(tgtEntry.getKey().equals(tgtMethod)){
                    // System.out.println("Location:"+tgtEntry.getValue()+" "+tgtEntry.getKey());
                    for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                        // System.out.println("in CallSite:"+srcEntry.getValue()+"--invoked by-->"+"Location:"+srcEntry.getKey().getJavaSourceStartLineNumber()+" "+srcEntry.getKey());
                        if(isExistingDp(sysInfo, srcEntry.getKey().getDeclaringClass().getName(), srcEntry.getKey().getSubSignature(), srcEntry.getValue())){
                            // System.out.println("Location:"+tgtEntry.getValue()+" "+tgtEntry.getKey());
                            // for(Map.Entry<SootMethod,Integer>srcEn:srcMap.entrySet()){
                            //     System.out.println("in CallSite:"+srcEn.getValue()+"--invoked by-->"+"Location:"+srcEn.getKey().getJavaSourceStartLineNumber()+" "+srcEn.getKey());
                            // }
                            // findDuplication(tgtMethod, csMap,srcEntry.getKey().getDeclaringClass().getName(), srcEntry.getKey().getSubSignature(), srcEntry.getValue());
                            // return false;

                            continue;
                        }
                        sysInfo.writeSystemInfo(this.sysName,srcEntry.getKey().getDeclaringClass().getName(),srcEntry.getKey().getSubSignature(),srcEntry.getValue(),0,null);

                    }
                }
            }
        }
    }

    public boolean isExistingDp(SystemInfo systemInfo,String classname,String funcname,int lineNum){
        Set<DelayPoint>dpSet=systemInfo.dpSet;
        Iterator<DelayPoint>dIterator=dpSet.iterator();
        while(dIterator.hasNext()){
            DelayPoint dp=dIterator.next();
            if(dp.className.equals(classname)&&dp.funcName.equals(funcname)&&dp.lineNum==lineNum){
                return true;
            }
        }
        return false;
    }
    
    /*
     * findDuplication
     * 
     */
    public void findDuplication(SootMethod tgtMethod,Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,String classname,String funcname,int lineNum){
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            Map<SootMethod,Integer> tgtMap=entry.getKey();
            Map<SootMethod,Integer> srcMap=entry.getValue();
            // assert tgtMap.size()==1;
            for(Map.Entry<SootMethod,Integer>tgtEntry:tgtMap.entrySet()){
                if(!tgtEntry.getKey().equals(tgtMethod)){
                    for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                        if(srcEntry.getKey().getDeclaringClass().getName().equals(classname)&&srcEntry.getKey().getSubSignature().equals(funcname)&&srcEntry.getValue()==lineNum){
                            System.out.println("MethodCallerAnalysis | Dupulication : Location:"+tgtEntry.getValue()+" "+tgtEntry.getKey());
                        }
                    }
                }
            }
        }
    }
}