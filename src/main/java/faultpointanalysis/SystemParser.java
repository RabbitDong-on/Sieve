package faultpointanalysis;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.StandardSystemProperty;

import config.SootConfig;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;
import jsonutils.*;


public class SystemParser{
    public static List<SootMethod> inLockMethodList=new ArrayList<>();
    public static void main( String[] args )throws Exception
    {
        SootConfig.setupSoot("zoo");
        SystemParser systemParser=new SystemParser();
        // systemParser.ObtainCaller("zoo");
    }

    /*
     * ObtainCaller
     */
    public void ObtainCaller(String sysName,String sysClassPath,int nodeId,int testOption){
        SystemInfo systemInfo=ParserSystem(sysName);
        Set<DelayPoint> delayPoints=systemInfo.dpSet;
        List<SootMethod>entryMethodList=ObtainMain(sysName);
        Scene.v().setEntryPoints(entryMethodList);
        String fileName=sysName+"Caller";
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTransform", new MethodCallerAnalysis(delayPoints, fileName, sysName,inLockMethodList,sysClassPath,nodeId,testOption)));
        PackManager.v().runPacks();
    }
    /*
     * ObtainMain
     */
    public List<SootMethod> ObtainMain(String sysName){
        List<SootMethod> smList=new ArrayList<>();
        List<SootClass> classList=ObtainSystemClass(sysName);
        List<SootMethod> methodList=new ArrayList<>();
        Iterator<SootClass>cIterator=classList.iterator();
        while(cIterator.hasNext()){
            SootClass sootClass=cIterator.next();
            methodList=sootClass.getMethods();
            Iterator<SootMethod>mIterator=methodList.iterator();
            while(mIterator.hasNext()){
                SootMethod sootMethod=mIterator.next();
                if(sootMethod.getName().equals("main")){
                    smList.add(sootMethod);
                }
            }
        }
        return smList;
    }

    /*
     * ObtainZookeeperClass
     */
    public List<SootClass> ObtainSystemClass(String sysName){
        // TODOEXTEND
        String packageName="org.apache.zookeeper";
        String packageName_plus="dgdg";
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
        Chain<SootClass> classChain=Scene.v().getClasses();
        List<SootClass> classList=new ArrayList<>();
        Iterator<SootClass>iter=classChain.iterator();
        while(iter.hasNext()){
            SootClass sootClass=iter.next();
            if(sootClass.getPackageName().contains(packageName)||sootClass.getPackageName().contains(packageName_plus)){
                classList.add(sootClass);
            }
        }
        // System.out.println("class in zookeeper num:"+classList.size()+"---"+classSet.size()+"-------"+filePath.size());
        return classList;
    }

    /*
     * ParserSystem
     */
    public SystemInfo ParserSystem(String sysName){
        List<SootClass>classList=ObtainSystemClass(sysName);
        // System.out.println("SystemParser | classList size:"+classList.size());
        Iterator<SootClass>classIter=classList.iterator();
        SystemInfo systemInfo=new SystemInfo();
        JsonUtil jsonUtil=new JsonUtil();
        while(classIter.hasNext()){
            SootClass sootClass=classIter.next();
            List<SootMethod>methodList=sootClass.getMethods();
            // System.out.println("SystemParser | methodList size:"+methodList.size());
            int count=methodList.size();
            for(int i=0;i<count;i++){
                SootMethod sootMethod=methodList.get(i);
                // if(sootMethod.getName().equals("doCommitOffsets")){
                //     System.out.println("SystemParser | doCommitOffsets:"+sootMethod);
                // }
                Map<Integer,List<SootClass>> lineNum2IOE=getLastLineNum(sootMethod);
                // lineNum2IOE.size();
                for(Map.Entry<Integer,List<SootClass>>entry:lineNum2IOE.entrySet()){
                    int lineNum=entry.getKey();
                    List<SootClass> ioRelatedEXP=entry.getValue();
                    systemInfo.writeSystemInfo(sysName, sootClass.getName(), sootMethod.getSubSignature(), lineNum,0,ioRelatedEXP);
                }    
            }
        }
        // delay point num : 1241 getLastLineNum
        // delay point num : 1708 getLineNum
        System.out.println("SystemParser | delay point num:"+systemInfo.dpSet.size());
        String path=System.getProperty("user.dir");
        jsonUtil.writeJson(path, sysName, systemInfo);
        return systemInfo;
    }
    /*
     * getLineNum
     */
    public static Map<Integer,List<SootClass>> getLineNum(SootMethod method){
        Set<Integer> lineNumSet=new HashSet<>();
        boolean isDebug=false;
        if(method.getName().equals("sendAndReceive")){
            isDebug=true;
        }   
        Map<Integer,List<SootClass>> lineNum2IOE=new LinkedHashMap<>();
        // boolean isContainInLock=false;
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(isDebug){
                    if(stmt.containsInvokeExpr()){
                        SootClass sc=stmt.getInvokeExpr().getMethod().getDeclaringClass();
                        if(sc.isInterface()&&sc.implementsInterface("java.io.Closeable")){
                            // System.out.println(stmt.getJavaSourceStartLineNumber()+":"+stmt);                 
                        }
                    }
                }
                if(stmt.containsInvokeExpr()){
                    if(stmt.getInvokeExpr().getMethod().getName().equals("inLock")&&!inLockMethodList.contains(method)){
                        // System.out.println("method:"+method);
                        inLockMethodList.add(method);
                    }
                }
                // bootstrap->apply->function->{true code}
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    SootMethod smethod=invokeExpr.getMethod();
                    if(smethod.getName().contains("bootstrap")){
                        SootClass sClass=smethod.getDeclaringClass();
                        List<SootMethod>lsm=sClass.getMethods();
                        Iterator<SootMethod> lsmIter=lsm.iterator();
                        while(lsmIter.hasNext()){
                            SootMethod sm=lsmIter.next();
                            if(sm.getName().contains("apply")){
                                // System.out.println("{}"+method.getSubSignature());
                                JimpleBody body1Body=(JimpleBody)sm.retrieveActiveBody();
                                BriefUnitGraph bug1=new BriefUnitGraph(body1Body);
                                for(Unit bb:bug1){
                                    Stmt tt=(Stmt)bb;
                                    if(tt.containsInvokeExpr()){
                                        SootMethod ssm=tt.getInvokeExpr().getMethod();
                                        if(!ssm.isConcrete()){
                                            continue;
                                        }
                                        JimpleBody body2Body=(JimpleBody)ssm.retrieveActiveBody();
                                        BriefUnitGraph bug2=new BriefUnitGraph(body2Body);
                                        for(Unit bbb:bug2){
                                            Stmt ttt=(Stmt)bbb;
                                            if(ttt.containsInvokeExpr()&&isRealIO(ttt)&&checkIO(ttt)){
                                                if(ttt.getJavaSourceStartLineNumber()!=-1){
                                                    // System.out.println("{}"+method.getSubSignature());
                                                    lineNumSet.add(ttt.getJavaSourceStartLineNumber());
                                                    InvokeExpr invokeExpr1=stmt.getInvokeExpr();
                                                    SootMethod sootMethod=invokeExpr1.getMethod();
                                                    List<SootClass>exceptionList=sootMethod.getExceptions();
                                                    Iterator<SootClass> exIterator=exceptionList.iterator();
                                                    List<SootClass> ioRelatedExceptionList=new ArrayList<>();
                                                    while(exIterator.hasNext()){
                                                        SootClass exception=exIterator.next();
                                                        if(isRelatedIOException(exception)){
                                                            // System.out.println("exception:"+exception.getName());
                                                            // System.out.println("invokeExper:"+method.getName());
                                                            ioRelatedExceptionList.add(exception);
                                                        }
                                                    }
                                                    lineNum2IOE.put(stmt.getJavaSourceStartLineNumber(), ioRelatedExceptionList);
                                                }
                                            }
                                        }
                                    }
                                }                          
                            }
                        }
                    }
                }
                if(stmt.containsInvokeExpr()&&isRealIO(stmt)&&checkIO(stmt)){
                    if(stmt.getJavaSourceStartLineNumber()!=-1){
                        lineNumSet.add(stmt.getJavaSourceStartLineNumber());
                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
                        SootMethod sootMethod=invokeExpr.getMethod();
                        List<SootClass>exceptionList=sootMethod.getExceptions();
                        Iterator<SootClass> exIterator=exceptionList.iterator();
                        List<SootClass> ioRelatedExceptionList=new ArrayList<>();
                        while(exIterator.hasNext()){
                            SootClass exception=exIterator.next();
                            if(isRelatedIOException(exception)){
                                // System.out.println("exception:"+exception.getName());
                                // System.out.println("invokeExper:"+method.getName());
                                ioRelatedExceptionList.add(exception);
                            }
                        }
                        lineNum2IOE.put(stmt.getJavaSourceStartLineNumber(), ioRelatedExceptionList);
                    }
                }
            }
        }
        return lineNum2IOE;
    }
    /*
     * isRelatedIOException
     */
    public static boolean isRelatedIOException(SootClass exception){
        if(exception.getName().equals("java.io.IOException")){
            return true;
        }
        if(exception.hasSuperclass()){
            SootClass superClass=exception.getSuperclass();
            if(superClass.getName().equals("java.io.IOException")){
                return true;
            }else{
                return isRelatedIOException(superClass);
            }
        }else{
            return false;
        }
    }
    /*
     * getLastLineNum
     */
    public static Map<Integer,List<SootClass>> getLastLineNum(SootMethod method){
        boolean isDebug=false; 
        Map<Integer,List<SootClass>> lineNum2IOE=new LinkedHashMap<>();
        // System.out.println("null");
        // if(method.getName().equals("doCommitOffsets")){
        //     isDebug=true;
        // }
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefBlockGraph bbg=new BriefBlockGraph(body);
            List<Block> bbList=bbg.getBlocks();
            Iterator<Block> bbIter=bbList.iterator();
            if(isDebug){
                for(Unit unit:body.getUnits()){
                    Stmt stmt=(Stmt)unit;
                    System.out.println("SystemParser | "+stmt.getJavaSourceStartLineNumber()+":"+stmt);
                    for(ValueBox valueBox:unit.getUseBoxes()){
                        Value value=valueBox.getValue();
                        if(value instanceof InvokeExpr){
                            InvokeExpr invokeExpr=(InvokeExpr)value;
                            SootMethodRef tgt=invokeExpr.getMethodRef();
                            System.out.println("SystemParser | tgt:"+tgt.getSignature());
                        }
                    }
                }
            }


            while(bbIter.hasNext()){
                Block bb=bbIter.next();
                Iterator<Unit> bbUIter=bb.iterator();
                int lastLineNum=-1;
                List<SootClass> ioRelatedExceptionList=new ArrayList<>();
                while(bbUIter.hasNext()){
                    Unit u=bbUIter.next();
                    Stmt stmt=(Stmt)u;
                    if(stmt.containsInvokeExpr()){
                        if(stmt.getInvokeExpr().getMethod().getName().equals("inLock")&&!inLockMethodList.contains(method)){
                            inLockMethodList.add(method);
                        }
                    }
                    if(stmt.containsInvokeExpr()){
                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
                        SootMethod smethod=invokeExpr.getMethod();
                        if(smethod.getName().contains("bootstrap")){
                            if(isDebug){
                                System.out.println("SystemParser bootstrap | lineNum:"+stmt.getJavaSourceStartLineNumber());
                            }
                            SootClass sClass=smethod.getDeclaringClass();
                            List<SootMethod>lsm=sClass.getMethods();
                            Iterator<SootMethod> lsmIter=lsm.iterator();
                            while(lsmIter.hasNext()){
                                SootMethod sm=lsmIter.next();
                                if(sm.getName().contains("apply")){
                                    if(isDebug){
                                        System.out.println("SystemParser apply | lineNum:"+stmt.getJavaSourceStartLineNumber());
                                    }
                                    JimpleBody body1Body=(JimpleBody)sm.retrieveActiveBody();
                                    BriefUnitGraph bug1=new BriefUnitGraph(body1Body);
                                    for(Unit bb1:bug1){
                                        Stmt tt=(Stmt)bb1;
                                        if(tt.containsInvokeExpr()){
                                            SootMethod ssm=tt.getInvokeExpr().getMethod();
                                            if(!ssm.isConcrete()){
                                                continue;
                                            }
                                            JimpleBody body2Body=(JimpleBody)ssm.retrieveActiveBody();
                                            BriefUnitGraph bug2=new BriefUnitGraph(body2Body);
                                            for(Unit bbb:bug2){
                                                Stmt ttt=(Stmt)bbb;
                                                if(ttt.containsInvokeExpr()&&isRealIO(ttt)&&checkIO(ttt)){
                                                    if(ttt.getJavaSourceStartLineNumber()!=-1){
                                                        lastLineNum=ttt.getJavaSourceStartLineNumber();
                                                        InvokeExpr invokeExpr1=ttt.getInvokeExpr();
                                                        SootMethod sootMethod=invokeExpr1.getMethod();
                                                        List<SootClass>exceptionList=sootMethod.getExceptions();
                                                        Iterator<SootClass> exIterator=exceptionList.iterator();
                                                        while(exIterator.hasNext()){
                                                            SootClass exception=exIterator.next();
                                                            if(isRelatedIOException(exception)){
                                                                ioRelatedExceptionList.add(exception);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }                          
                                }
                            }
                        }
                    }
                    if(stmt.containsInvokeExpr()){
                        if((isRealIO(stmt)&&checkIO(stmt))){
                            if(stmt.getJavaSourceStartLineNumber()!=-1){
                                lastLineNum=stmt.getJavaSourceStartLineNumber();
                                InvokeExpr invokeExpr=stmt.getInvokeExpr();
                                SootMethod sootMethod=invokeExpr.getMethod();
                                List<SootClass>exceptionList=sootMethod.getExceptions();
                                Iterator<SootClass> exIterator=exceptionList.iterator();
                                while(exIterator.hasNext()){
                                    SootClass exception=exIterator.next();
                                    if(isRelatedIOException(exception)){
                                        ioRelatedExceptionList.add(exception);
                                    }
                                }
                            }
                        }
                    }
                }
                if(lastLineNum!=-1){
                    lineNum2IOE.put(lastLineNum, ioRelatedExceptionList);
                }
            }
        }
        return lineNum2IOE;
    }
    /*
    * checkIO
    */
    public static boolean checkIO(Stmt stmt){
        String className=stmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
        SootClass sc=stmt.getInvokeExpr().getMethod().getDeclaringClass();
        if(sc.isInterface()&&sc.implementsInterface("java.io.Closeable")){
            return true;
        }
        if(className.contains("java.io")||className.contains("java.nio")||
        className.contains("io.netty")||className.contains("java.net")||
        className.contains("javax.net")){
            return true;
        }
        if(className.contains("org.apache.jute.Record")||
        className.contains("org.apache.jute.OutputArchive")||
        className.contains("org.apache.jute.InputArchive")||
        className.contains("org.apache.jute.BinaryOutputArchive")||
        className.contains("org.apache.jute.BinaryInputArchive")){
            return true;
        }
        if(className.contains("org.apache.hadoop.fs")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.AliasMapProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.DatanodeLifelineProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.InterDatanodeProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.JournalProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.NamenodeProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.ClientDatanodeProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.ReconfigurationProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.protocolPB.RouterAdminProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.qjournal.protocolPB.InterQJournalProtocolPB")||
        className.contains("org.apache.hadoop.hdfs.qjournal.protocolPB.QJournalProtocolPB")
        ){
            return true;
        }
        if(className.contains("org.apache.hadoop.hbase.ipc.AbstractRpcClient$BlockingRpcChannelImplementation")){
            return true;
        }
        if(className.contains("org.apache.hadoop.mapreduce.protocol.ClientProtocol")||
        className.contains("org.apache.hadoop.mapred.TaskUmbilicalProtocol")||
        className.contains("prg.apache.hadoop.mapreduce.v2.api.MRClientProtocolPB")||
        className.contains("org.apache.hadoop.mapreduce.v2.api.HSClientProtocolPB")||
        className.contains("org.apache.hadoop.mapreduce.v2.api.HSAdminRefreshProtocolPB")
        ){
            return true;
        }
        return false;
    }
    /*
     * isRealIO
     */
    public static boolean isRealIO(Stmt stmt){
        if(stmt.getInvokeExpr().getMethod().getName().equals("println")){
            return false;
        }
        if(stmt.getInvokeExpr().getMethod().getName().equals("printf")){
            return false;
        }
        return true;
    }
}