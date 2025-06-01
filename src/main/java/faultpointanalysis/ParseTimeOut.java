package faultpointanalysis;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import config.SootConfig;
import jsonutils.DelayPoint;
import jsonutils.SystemInfo;
import jsonutils.TimeOutPoint;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.IfStmt;
import soot.jimple.AddExpr;
import soot.jimple.AssignStmt;
import soot.jimple.CmpExpr;
import soot.jimple.DivExpr;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.MulExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.TransitiveTargets;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;

public class ParseTimeOut{

    public String packageName;
    public String packageName_plus="dgdg";
    public ParseTimeOut(String packageName,String packageName_plus){
        this.packageName=packageName;
        this.packageName_plus=packageName_plus;
    }
    List<String> timeoutFieldObj=new ArrayList<>();
    Map<String,Map<Integer,SootMethod>>timeoutField2PosM=new LinkedHashMap<>();
    public static void main(String[] args){
        SootConfig.setupSoot("faultinjection.ParseTimeOut");
        ParseTimeOut ParseTimeOut=new ParseTimeOut("1","2");
        Map<SootMethod,List<TimeOutPoint>>timeout2Info=ParseTimeOut.parseTimeoutMethod();
        // List<SootMethod> timeoutMethodList=ParseTimeOut.ParseTimeOutMethod();
        // 79
        System.out.println("ParseTimeOut.java | size:"+timeout2Info.size());

        Map<SootMethod,Set<Integer>> timeoutMethod2Handler=new LinkedHashMap<>();
        // timeoutMethod2Handler=ParseTimeOut.ParseTimeOutHandler(timeout2Info);
        System.out.println(timeoutMethod2Handler.size());
        // Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap=new LinkedHashMap<>();
        // List<SootMethod> ioTimeOutMethodList=ParseTimeOut.parseIORelatedTimeOutMethod(csMap,timeoutMethodList);
    }
    /*
     * connect(timeout)
     * send(time)
     */
    public List<SootMethod> parseTOSIO(Set<DelayPoint> dpSet){
        List<SootMethod> res=new ArrayList<>();
        Iterator<DelayPoint> dpIterator=dpSet.iterator();
        while(dpIterator.hasNext()){
            DelayPoint dp=dpIterator.next();
            SootClass tgtSootClass=Scene.v().forceResolve(dp.className, SootClass.BODIES);
            SootMethod tgtMethod=tgtSootClass.getMethod(dp.funcName);
            int lineNum=dp.lineNum;
            if(tgtMethod.getName().equals("XXXX")&&lineNum==94){
                res.add(tgtMethod);
            }
        }
        return res;
    }

    public Map<SootMethod,List<TimeOutPoint>> parseTimeoutMethod(){
        Chain<SootClass>classChain=Scene.v().getClasses();
        List<SootMethod>methodList=new ArrayList<>();
        Map<SootMethod,List<TimeOutPoint>> timeoutInfo=new LinkedHashMap<>();
        List<SootMethod>generalList=new ArrayList<>();
        // Set<TimeOutPoint> toSet=new HashSet<>();
        Iterator<SootClass>cIterator=classChain.iterator();
        while(cIterator.hasNext()){
            SootClass sootClass=cIterator.next();
            if(!isSystemClass(sootClass)){
                continue;
            }
            List<SootMethod> methodChain=sootClass.getMethods();
            Iterator<SootMethod> mIterator=methodChain.iterator();
            while(mIterator.hasNext()){
                SootMethod sootMethod=mIterator.next();
                List<TimeOutPoint> toPList=isTimeOutMethod(sootMethod);
                if(isGeneralTimeOutMethod(sootMethod)){
                    if(!generalList.contains(sootMethod)){
                        generalList.add(sootMethod);
                    }
                }
                if(toPList.size()!=0&&!methodList.contains(sootMethod)){
                    // System.out.println("ParseTimeOut | name:"+sootMethod.getSignature());
                    methodList.add(sootMethod);
                    timeoutInfo.put(sootMethod, toPList);
                }
            }
        }

        System.out.println("ParseTimeOut | timeout2Info(without general):"+timeoutInfo.size());
        System.out.println("ParseTimeOut | generalTimeOutMethod:"+generalList.size());

        Iterator<SootMethod> genIter=generalList.iterator();
        while(genIter.hasNext()){
            SootMethod timeoutMethod=genIter.next();
            Map<String,Map<Integer,SootMethod>> field2PosM=parseTimeOutField(timeoutMethod);
            for(Map.Entry<String,Map<Integer,SootMethod>>entry:field2PosM.entrySet()){
                String field=entry.getKey();
                Map<Integer,SootMethod> pos2Method=entry.getValue();
                if(!timeoutFieldObj.contains(field)){
                    timeoutFieldObj.add(field);
                    timeoutField2PosM.put(field, pos2Method);
                }else{
                    Map<Integer,SootMethod> oldValue=timeoutField2PosM.get(field);
                    Map<Integer,SootMethod> newValue=new LinkedHashMap<>();
                    newValue.putAll(oldValue);
                    newValue.putAll(pos2Method);
                    timeoutField2PosM.replace(field, oldValue, newValue);
                }
            }
        }
        System.out.println("ParseTimeOut | timeoutFieldObj:"+timeoutFieldObj.size());
        genIter=generalList.iterator();
        int duplicatedCount=0;
        int newCount=0;
        while(genIter.hasNext()){
            SootMethod genMethod=genIter.next();
            List<TimeOutPoint> toPList=obtainGeneralTimeOutPoint(genMethod,timeoutFieldObj);
            if(toPList.size()!=0){
                // System.out.println("ParseTimeOut | name:"+genMethod.getSignature());
                List<TimeOutPoint>etoPList=timeoutInfo.get(genMethod);
                if(etoPList!=null){
                    duplicatedCount++;
                    toPList.addAll(etoPList);
                    timeoutInfo.remove(genMethod,etoPList);
                    timeoutInfo.put(genMethod, toPList);
                }else{
                    newCount++;
                    timeoutInfo.put(genMethod, toPList);
                }
            }
        }
        System.out.println("ParseTimeOut | duplicated : "+duplicatedCount+" new : "+newCount);
        return timeoutInfo;
    }


    public Map<SootMethod,List<SootMethod>> ObtainGen2IO(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,List<SootMethod>analyzedIO,Map<SootMethod,Map<SootMethod,List<SootMethod>>> gen2Scope){
        Map<SootMethod,List<SootMethod>> res=new LinkedHashMap<>();
        for(Map.Entry<SootMethod,Map<SootMethod,List<SootMethod>>>entry:gen2Scope.entrySet()){
            SootMethod genMethod=entry.getKey();
            Map<SootMethod,List<SootMethod>> scope=entry.getValue();
            List<SootMethod>tmpList=new ArrayList<>();
            for(Map.Entry<SootMethod,List<SootMethod>>entry2:scope.entrySet()){
                SootMethod endMethod=entry2.getKey();
                List<SootMethod> startMethodList=entry2.getValue();

                if(startMethodList.contains(endMethod)&&startMethodList.size()==1){
                    tmpList.add(endMethod);
                }else{
                    Iterator<SootMethod> startIter=startMethodList.iterator();
                    while(startIter.hasNext()){
                        SootMethod method=startIter.next();
                        if(!method.equals(endMethod)){
                            tmpList.add(method);
                        }
                    }
                }
            }
            List<SootMethod> tgtList=new ArrayList<>();
            for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry2:csMap.entrySet()){
                for(Map.Entry<SootMethod,Integer>entry3:entry2.getKey().entrySet()){
                    SootMethod method=entry3.getKey();
                    if(tmpList.contains(method)&&!tgtList.contains(method)){
                        tgtList.add(method);
                    }
                }
            }
            List<SootMethod>ioList=color(csMap, tgtList, analyzedIO);
            if(ioList.size()!=0){
                res.put(genMethod, ioList);
            }
        }
        return res;
    }
    public  Map<SootMethod,List<SootMethod>> ObtainWAT2IO(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,List<SootMethod>analyzedIO,Map<SootMethod,Map<SootMethod,Integer>> wat2NSR,Set<DelayPoint>dpSet){
        Map<SootMethod,List<SootMethod>>timeout2IO=new LinkedHashMap<>();
        for(Map.Entry<SootMethod,Map<SootMethod,Integer>>entry:wat2NSR.entrySet()){
            SootMethod watMethod=entry.getKey();
            Map<SootMethod,Integer> nsrMethod2Pos=entry.getValue();
            List<SootMethod> tgtList=new ArrayList<>();
            for(Map.Entry<SootMethod,Integer>entry1:nsrMethod2Pos.entrySet()){
                SootMethod nsrMethod=entry1.getKey();
                int pos=entry1.getValue();
                for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csEntry:csMap.entrySet()){
                    Map<SootMethod,Integer> srcMap=csEntry.getValue();
                    for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                        SootMethod srcMethod=srcEntry.getKey();
                        int tgtPos=srcEntry.getValue();
                        if(srcMethod.equals(nsrMethod)&&tgtPos<pos&&!tgtList.contains(srcMethod)){

                            tgtList.add(srcMethod);
                        }
                    }
                }
                Iterator<DelayPoint> dpIter=dpSet.iterator();
                while(dpIter.hasNext()){
                    DelayPoint dp=dpIter.next();
                    if(nsrMethod.getDeclaringClass().getName().equals(dp.className)&&
                    nsrMethod.getSubSignature().equals(dp.funcName)&&
                    dp.lineNum<pos){
                        tgtList.add(nsrMethod);
                    }
                }
            }
            List<SootMethod> ioList=color(csMap, tgtList, analyzedIO);
            if(ioList.size()!=0){
                timeout2IO.put(watMethod, ioList);
            }
        }
        return timeout2IO;
    }
    public  Map<SootMethod,List<SootMethod>>  ObtainJoin2IO(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,List<SootMethod>analyzedIO,Map<SootMethod,List<SootMethod>> join2Run){
        Map<SootMethod,List<SootMethod>>timeout2IO=new LinkedHashMap<>();
        for(Map.Entry<SootMethod,List<SootMethod>>entry:join2Run.entrySet()){
            SootMethod joinMethod=entry.getKey();
            List<SootMethod> runMethod=entry.getValue();
            List<SootMethod> tgtList=new ArrayList<>();
            tgtList.addAll(runMethod);
            List<SootMethod> ioList=color(csMap, tgtList, analyzedIO);
            if(ioList.size()!=0){
                timeout2IO.put(joinMethod, ioList);
            }
        }
        return timeout2IO;
    }

    public List<SootMethod> color(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,List<SootMethod> tgtList,List<SootMethod>analyzedIO){
        List<SootMethod> res=new ArrayList<>();
        List<SootMethod> curColorList=new ArrayList<>();
        List<SootMethod> nextColorList=new ArrayList<>();
        nextColorList.addAll(tgtList);
        while(nextColorList.size()!=0){
            curColorList.addAll(nextColorList);
            nextColorList.removeAll(nextColorList);
            for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
                Map<SootMethod,Integer> tgtMap=entry.getKey();
                Map<SootMethod,Integer> srcMap=entry.getValue();
                for(Map.Entry<SootMethod,Integer>tgtEntry:tgtMap.entrySet()){
                    SootMethod tgtMethod=tgtEntry.getKey();
                    for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                        SootMethod srcMethod=srcEntry.getKey();
                        if(curColorList.contains(srcMethod)&&!curColorList.contains(tgtMethod)&&!nextColorList.contains(tgtMethod)){
                            nextColorList.add(tgtMethod);
                        }
                    }
                }
            }
        }
        Iterator<SootMethod>iter=analyzedIO.iterator();
        while(iter.hasNext()){
            SootMethod analyzedIOMethod=iter.next();
            if(curColorList.contains(analyzedIOMethod)){
                res.add(analyzedIOMethod);
            }
        }
        return res;
    }


    public boolean isSystemClass(SootClass sootClass){
        // TODOEXTEND
        if(sootClass.getPackageName().contains(packageName)||sootClass.getPackageName().contains(packageName_plus)){
            return true;
        }
        return false;
    }

    public List<TimeOutPoint> isTimeOutMethod(SootMethod method){
        List<TimeOutPoint> toPList=new ArrayList<>();
        InjectInfo info=null;
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                TimeOutPoint toP=new TimeOutPoint();
                int type=-1;
                int pos=-1;
                Stmt stmt=(Stmt)u;
                pos=stmt.getJavaSourceStartLineNumber();
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();

                    if(invokeExpr.getMethod().getSubSignature().equals("void wait(long)")||
                       invokeExpr.getMethod().getSubSignature().equals("void wait(long,int)")){
                        // System.out.println("ParseTimeOut | lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                        type=2;
                    }
                    if(invokeExpr.getMethod().getSubSignature().equals("boolean await(long,java.util.concurrent.TimeUnit)")||
                       invokeExpr.getMethod().getSubSignature().equals("long awaitNanos(long)")){
                        type=4;
                        // System.out.println("ParseTimeOut | lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                    }
                    if(invokeExpr.getMethod().getSubSignature().equals("boolean tryAcquire(long,java.util.concurrent.TimeUnit)")){
                        type=5;
                        // System.out.println("ParseTimeOut | lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                    }
                    if(invokeExpr.getMethod().getSubSignature().equals("void join(long)")||
                       invokeExpr.getMethod().getSubSignature().equals("void join(long,int)")){
                        // System.out.println("ParseTimeOut | find join!");
                        type=6;
                        // System.out.println("ParseTimeOut | lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                    }
                    // if(invokeExpr.getMethod().getSubSignature().equals("boolean tryLock(long,java.util.concurrent.TimeUnit)")){
                    //     type=7;
                    //     break;
                    // }
                }
                if(type!=-1){
                    toP.timeoutMethodType=type;
                    if(type==3&&info!=null){
                        toP.toPos=info.pos;
                    }else{
                        toP.toPos=pos;
                    }
                    toP.toClassName=method.getDeclaringClass().getName();
                    toP.toFuncName=method.getSubSignature();
                    toPList.add(toP);
                }
            }
        }
        return toPList;
    }
    public boolean isGeneralTimeOutMethod(SootMethod method){
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    // invokeExpr.getMethod().getSubSignature().equals("long currentWallTime()")||
                    // invokeExpr.getMethod().getSubSignature().equals("java.util.Date elapsedTimeToDate(long)"
                    if(invokeExpr.getMethod().getSubSignature().equals("long nanoTime()")||
                    invokeExpr.getMethod().getSubSignature().equals("long milliseconds()")||
                    invokeExpr.getMethod().getSubSignature().equals("long nanoseconds()")||
                    invokeExpr.getMethod().getSubSignature().equals("long currentTimeMs()")||
                    invokeExpr.getMethod().getSubSignature().equals("long now()")||
                    invokeExpr.getMethod().getSubSignature().equals("long monotonicNow()")||
                    invokeExpr.getMethod().getSubSignature().equals("long getTime()")||
                    invokeExpr.getMethod().getSubSignature().equals("long currentTime()")||
                    invokeExpr.getMethod().getSubSignature().equals("long getTimeInMillis()")||
                    invokeExpr.getMethod().getSubSignature().equals("long currentTimeMillis()")||
                    invokeExpr.getMethod().getSubSignature().equals("long currentWallTime()")||
                    invokeExpr.getMethod().getSubSignature().equals("long currentElapsedTime()")){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public List<TimeOutPoint> obtainGeneralTimeOutPoint(SootMethod method,List<String>timeoutFieldList){
        List<TimeOutPoint> toPList=new ArrayList<>();
        List<InjectInfo> info=new ArrayList<>();
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    // invokeExpr.getMethod().getSubSignature().equals("long currentWallTime()")||
                    // invokeExpr.getMethod().getSubSignature().equals("java.util.Date elapsedTimeToDate(long)"
                    if(invokeExpr.getMethod().getSubSignature().equals("long currentWallTime()")||
                        invokeExpr.getMethod().getSubSignature().equals("long currentElapsedTime()")){

                        info=parseCustomizedTimeOut(method,timeoutFieldList);
                        // System.out.println("ParseTimeOut | info:"+info.size());
                        // if(info.size()!=0){
                        //     System.out.println("className:"+method.getDeclaringClass().getName());
                        //     System.out.println("funcName:"+method.getName());
                        //     Iterator<InjectInfo> injectInfoIter=info.iterator();
                        //     while(injectInfoIter.hasNext()){
                        //         InjectInfo injectInfo=injectInfoIter.next();
                        //         System.out.println("info:"+injectInfo.pos);
                        //     }
                        // }
                    }
                }
            }
            if(info.size()!=0){
                Iterator<InjectInfo> iIter=info.iterator();
                while(iIter.hasNext()){
                    InjectInfo injectInfo=iIter.next();
                    TimeOutPoint toP=new TimeOutPoint();
                    toP.traceType=injectInfo.traceType;
                    toP.lr=injectInfo.lr;
                    toP.toPos=injectInfo.pos;
                    toP.cmpPos=injectInfo.cmpPos;
                    toP.timeoutMethodType=3;
                    toP.toClassName=method.getDeclaringClass().getName();
                    toP.toFuncName=method.getSubSignature();
                    toPList.add(toP);
                }

            }
        }
        return toPList;
    }
    public boolean isDuplicated(InjectInfo curinfo,List<InjectInfo>infoList){
        Iterator<InjectInfo> infoIter=infoList.iterator();
        while(infoIter.hasNext()){
            InjectInfo info=infoIter.next();
            if((info.lr==curinfo.lr)&&(info.pos==curinfo.pos)&&(info.traceType==curinfo.traceType)){
                return true;
            }
        }
        return false;
    }

    public List<InjectInfo> parseCustomizedTimeOut(SootMethod sootMethod,List<String>timeoutFieldList){
        List<InjectInfo> res=new ArrayList<>();
        JimpleBody body=(JimpleBody)sootMethod.retrieveActiveBody();
        List<String> timeoutVar=new ArrayList<>();
        List<ValueInfo> varInfo=new ArrayList<>();
        boolean isDebug=false;
        if(sootMethod.getDeclaringClass().getName().contains("ContainerManager")&&sootMethod.getName().equals("checkContainers")){
            isDebug=false;
        }
        for(Unit u:body.getUnits()){
            Stmt stmt=(Stmt)u;
            if(isDebug){
                System.out.println(stmt.getJavaSourceStartLineNumber()+":"+stmt);
            }
            if(stmt instanceof AssignStmt){
                AssignStmt assignStmt=(AssignStmt)stmt;
                Value lV=assignStmt.getLeftOp();
                Value rV=assignStmt.getRightOp();
                if(stmt.containsFieldRef()){
                    FieldRef fieldRef=stmt.getFieldRef();
                    SootField field=fieldRef.getField();
                    if(timeoutFieldList.contains(field.getSignature())){
                        if(rV.toString().contains(field.getSignature())){
                            if(!timeoutVar.contains(lV.toString())){
                                timeoutVar.add(lV.toString());
                                ValueInfo valueInfo=new ValueInfo();
                                valueInfo.type=1;
                                valueInfo.value=lV.toString();
                                valueInfo.v=lV;
                                varInfo.add(valueInfo);
                                if(isDebug){
                                    System.out.println(lV.toString());
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : lV=field type=1:"+stmt);
                                }
                            }
                        }
                    }
                }
            }
            if(stmt.containsInvokeExpr()){
                InvokeExpr invokeExpr=stmt.getInvokeExpr();
                if(invokeExpr.getMethod().getSubSignature().equals("long currentWallTime()")||
                    invokeExpr.getMethod().getSubSignature().equals("long currentElapsedTime()")){
                    List<ValueBox> defBoxes=stmt.getDefBoxes();
                    Iterator<ValueBox> defIter=defBoxes.iterator();
                    while(defIter.hasNext()){
                        ValueBox vb=defIter.next();
                        if(!timeoutVar.contains(vb.getValue().toString())){
                            timeoutVar.add(vb.getValue().toString());
                            ValueInfo valueInfo=new ValueInfo();
                            valueInfo.v=vb.getValue();
                            valueInfo.value=vb.getValue().toString();
                            valueInfo.type=1;
                            varInfo.add(valueInfo);
                            if(isDebug){
                                System.out.println(vb.getValue().toString());
                                System.out.println(stmt.getJavaSourceStartLineNumber()+" : lV=System.Time() type=1:"+stmt);
                            }
                        }else{
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.value.equals(vb.getValue().toString())){
                                    vi.type=1;
                                    if(isDebug){
                                        System.out.println(stmt.getJavaSourceStartLineNumber()+" : lV=System.Time() type=1:"+stmt);
                                    }
                                    break;
                                }
                            }

                        }
                    }   
                }
            }
            if(stmt instanceof AssignStmt){
                AssignStmt assignStmt=(AssignStmt)stmt;
                Value lV=assignStmt.getLeftOp();
                Value rV=assignStmt.getRightOp();      

                if(rV instanceof CmpExpr){
                    // System.out.println("Cmp:"+stmt);
                    CmpExpr ce=(CmpExpr)rV;
                    Value lo=ce.getOp1();
                    Value ro=ce.getOp2();
                    // int type=0;
                    if((timeoutVar.contains(lo.toString())&&!timeoutVar.contains(ro.toString()))){
                        Iterator<ValueInfo> viIter=varInfo.iterator();
                        while(viIter.hasNext()){
                            ValueInfo vi=viIter.next();
                            if(vi.value.equals(lo.toString())&&vi.type==2){
                                InjectInfo info=new InjectInfo();
                                info.lr=2;
                                info.pos=stmt.getJavaSourceStartLineNumber();
                                info.cmpPos=stmt.getJavaSourceStartLineNumber();
                                info.traceType=2;
                                if(!isDuplicated(info, res)){
                                    res.add(info);
                                    if(isDebug){
                                        System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP toValue:"+stmt);
                                    }
                                }
                                
                                // type=2;
                            }else if(vi.value.equals(lo.toString())&&vi.type==3){
                                // if(ro.toString().equals("0")||ro.toString().equals("0L")){
                                //     System.out.println("exclude: ro:"+ro+" method:"+sootMethod.getSubSignature());
                                //     break;
                                // }else{
                                //     System.out.println("failed: ro:"+ro);
                                // }
                                InjectInfo info=new InjectInfo();
                                info.lr=vi.lr;
                                info.pos=vi.pos;
                                info.cmpPos=stmt.getJavaSourceStartLineNumber();
                                info.traceType=1;
                                if(!isDuplicated(info, res)){
                                    res.add(info);
                                    if(isDebug){
                                        System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP toValue:"+stmt);
                                    }
                                }
                            }
                        }
                    }else if((!timeoutVar.contains(lo.toString())&&timeoutVar.contains(ro.toString()))){
                        Iterator<ValueInfo> viIter=varInfo.iterator();
                        while(viIter.hasNext()){
                            ValueInfo vi=viIter.next();
                            if(vi.value.equals(ro.toString())&&vi.type==2){
                                InjectInfo info=new InjectInfo();
                                info.lr=1;
                                info.pos=stmt.getJavaSourceStartLineNumber();
                                info.cmpPos=stmt.getJavaSourceStartLineNumber();
                                info.traceType=2;
                                if(!isDuplicated(info, res)){
                                    res.add(info);
                                    if(isDebug){
                                        System.out.println(stmt.getJavaSourceStartLineNumber()+" : toValue CMP timeoutVar:"+stmt);
                                    }
                                }
                                // type=2;
                            }else if(vi.value.equals(lo.toString())&&vi.type==3){
                                // if(lo.toString().equals("0")||lo.toString().equals("0L")){
                                //     System.out.println("exclude: lo:"+lo+" method:"+sootMethod.getSubSignature());
                                //     break;
                                // }else{
                                //     System.out.println("failed: lo:"+lo);
                                // }
                                InjectInfo info=new InjectInfo();
                                info.lr=vi.lr;
                                info.pos=vi.pos;
                                info.cmpPos=stmt.getJavaSourceStartLineNumber();
                                info.traceType=1;
                                if(!isDuplicated(info, res)){
                                    res.add(info);
                                    if(isDebug){
                                        System.out.println(stmt.getJavaSourceStartLineNumber()+" : toValue CMP timeoutVar:"+stmt);
                                    }
                                }
                            }
                        }
                    }else if(timeoutVar.contains(lo.toString())&&timeoutVar.contains(ro.toString())){
                        Iterator<ValueInfo> viIter=varInfo.iterator();
                        while(viIter.hasNext()){
                            ValueInfo vi=viIter.next();
                            if(vi.value.equals(lo.toString())&&vi.type==3){
                                if(vi.toValue==null){
                                    System.out.println("ParseTimeOut | ---error---");
                                    System.exit(0);
                                }
                                InjectInfo info=new InjectInfo();
                                info.lr=vi.lr;
                                info.pos=vi.pos;
                                info.cmpPos=stmt.getJavaSourceStartLineNumber();
                                info.traceType=1;
                                if(!isDuplicated(info, res)){
                                    res.add(info);
                                    if(isDebug){
                                        System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP timeoutVar:"+stmt);
                                    }
                                }
                            }
                            if(vi.value.equals(ro.toString())&&vi.type==3){
                                if(vi.toValue==null){
                                    System.out.println("ParseTimeOut.java | ---error---");
                                    System.exit(0);
                                }
                                InjectInfo info=new InjectInfo();
                                info.lr=vi.lr;
                                info.pos=vi.pos;
                                info.cmpPos=stmt.getJavaSourceStartLineNumber();
                                info.traceType=1;
                                    if(!isDuplicated(info, res)){
                                    res.add(info);
                                    if(isDebug){
                                        System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP timeoutVar:"+stmt);
                                    }
                                }
                            }
                        }
                    }else{
                        continue;
                    }
                }else if((rV instanceof DivExpr)||(rV instanceof MulExpr)){
                    // System.out.println("Div|Mul"+stmt);
                    Value lo,ro;
                    int type=0;
                    if(rV instanceof DivExpr){
                        DivExpr divExpr=(DivExpr)rV;
                        lo=divExpr.getOp1();
                        ro=divExpr.getOp2();
                    }else{
                        MulExpr mulExpr=(MulExpr)rV;
                        lo=mulExpr.getOp1();
                        ro=mulExpr.getOp2();
                    }
                    if(timeoutVar.contains(lo.toString())){
                        Iterator<ValueInfo> vIter=varInfo.iterator();
                        while(vIter.hasNext()){
                            ValueInfo vi=vIter.next();
                            if(vi.value.equals(lo.toString())){
                                type=vi.type;
                            }
                        }
                    }else if(timeoutVar.contains(ro.toString())){
                        Iterator<ValueInfo> vIter=varInfo.iterator();
                        while(vIter.hasNext()){
                            ValueInfo vi=vIter.next();
                            if(vi.value.equals(ro.toString())){
                                type=vi.type;
                            }
                        }
                    }else{
                        continue;
                    }
                    if(timeoutVar.contains(lV.toString())){
                        Iterator<ValueInfo> vIter=varInfo.iterator();
                        while(vIter.hasNext()){
                            ValueInfo vi=vIter.next();
                            if(vi.value.equals(lV.toString())){
                                vi.type=type;
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : LV Div|Mul timeoutVar type="+type+":"+stmt);
                                }
                                break;
                            }
                        }
                    }else{
                        timeoutVar.add(lV.toString());
                        ValueInfo valueInfo=new ValueInfo();
                        valueInfo.v=lV;
                        valueInfo.type=type;
                        valueInfo.value=lV.toString();
                        varInfo.add(valueInfo);
                        if(isDebug){
                            System.out.println(lV.toString());
                            System.out.println(stmt.getJavaSourceStartLineNumber()+" : LV Div|Mul timeoutVar type="+type+":"+stmt);
                        }
                    }
                }else if((rV instanceof AddExpr)||(rV instanceof SubExpr)){
                    // System.out.println("Add|Sub:"+stmt);
                    Value lo,ro;
                    Value tValue=null;
                    int type=0;
                    int lr=0;
                    if(rV instanceof AddExpr){
                        AddExpr addExpr=(AddExpr)rV;
                        lo=addExpr.getOp1();
                        ro=addExpr.getOp2();
                    }else{
                        SubExpr subExpr=(SubExpr)rV;
                        lo=subExpr.getOp1();
                        ro=subExpr.getOp2();
                    }
                    if(timeoutVar.contains(lo.toString())&&timeoutVar.contains(ro.toString())&&(rV instanceof SubExpr)){
                        type=2;
                    }else if(timeoutVar.contains(lo.toString())&&!timeoutVar.contains(ro.toString())){
                        type=3;
                        tValue=ro;
                        lr=2;
                    }else if(!timeoutVar.contains(lo.toString())&&timeoutVar.contains(ro.toString())){
                        type=3;
                        tValue=lo;
                        lr=1;
                    }else{
                        continue;
                    }
                    if(!timeoutVar.contains(lV.toString())){
                        ValueInfo valueInfo=new ValueInfo();
                        valueInfo.v=lV;
                        valueInfo.type=type;
                        valueInfo.value=lV.toString();
                        if(tValue!=null){
                            valueInfo.toValue=tValue;
                            valueInfo.pos=stmt.getJavaSourceStartLineNumber();
                            valueInfo.lr=lr;
                        }
                        varInfo.add(valueInfo);
                        timeoutVar.add(lV.toString());
                        if(isDebug){
                            System.out.println(stmt.getJavaSourceStartLineNumber()+" : LV Add|Sub timeoutVar type="+type+":"+stmt);
                        }
                    }else{
                        Iterator<ValueInfo> vIter=varInfo.iterator();
                        while(vIter.hasNext()){
                            ValueInfo vi=vIter.next();
                            if(vi.value.equals(lV.toString())){
                                vi.type=type;
                                if(tValue!=null){
                                    vi.toValue=tValue;
                                    vi.pos=stmt.getJavaSourceStartLineNumber();
                                    vi.lr=lr;
                                }
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : LV Add|Sub timeoutVar type="+type+":"+stmt);
                                }
                                break;
                            }
                        }
                    }
                }else{
                    List<ValueBox> useBoxes=rV.getUseBoxes();
                    // if(isDebug){
                    //     if(stmt.getJavaSourceStartLineNumber()==1215){
                    //         System.out.println("useBoxes:"+useBoxes.size()+" rV:"+rV);
                    //         System.out.println(useBoxes.get(0).getValue());
                    //     }
                    // }                  
                    if(useBoxes.size()==0||useBoxes.size()==1){
                        Value tmp;
                        if(useBoxes.size()==0){
                            tmp=rV;
                        }else{
                            tmp=useBoxes.get(0).getValue();
                        }
                        int type=0;
                        Value tValue=null;
                        int pos=0;
                        int lr=0;
                        if(timeoutVar.contains(tmp.toString())){
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            boolean isFind=false;
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.value.equals(tmp.toString())){
                                    type=vi.type;
                                    tValue=vi.toValue;
                                    pos=vi.pos;
                                    lr=vi.lr;
                                    isFind=true;
                                    break;
                                }
                            }
                            if(!isFind){
                                continue;
                            }
                            if(!timeoutVar.contains(lV.toString())){
                                ValueInfo valueInfo=new ValueInfo();
                                valueInfo.v=lV;
                                valueInfo.value=lV.toString();
                                valueInfo.type=type;
                                if(tValue!=null){
                                    valueInfo.toValue=tValue;
                                    valueInfo.pos=pos;
                                    valueInfo.lr=lr;
                                }
                                varInfo.add(valueInfo);
                                timeoutVar.add(lV.toString());
                                if(isDebug){
                                    System.out.println(lV.toString());
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : LV = timeoutVar type="+type+":"+stmt);
                                }
                            }else{
                                Iterator<ValueInfo> viIter=varInfo.iterator();
                                while(viIter.hasNext()){
                                    ValueInfo vi=viIter.next();
                                    if(vi.value.equals(lV.toString())){
                                        vi.type=type;
                                        if(tValue!=null){
                                            vi.toValue=tValue;
                                            vi.pos=pos;
                                            vi.lr=lr;
                                        }
                                        if(isDebug){
                                            System.out.println(stmt.getJavaSourceStartLineNumber()+" : LV = timeoutVar type="+type+":"+stmt);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }else{
                        // LV=staticinvoke(timeoutVar);
                        // timeoutVar type = 3 other not process
                        if (rV instanceof StaticInvokeExpr){
                            StaticInvokeExpr staticInvokeExpr=(StaticInvokeExpr)rV;
                            List<Value> args=staticInvokeExpr.getArgs();
                            if(isDebug){
                                System.out.println(args);
                            }
                            int type=0;
                            Value tValue=null;
                            int pos=0;
                            int lr=0;
                            Iterator<Value> argIter=args.iterator();
                            while (argIter.hasNext()) {
                                Value arg=argIter.next();
                                if(timeoutVar.contains(arg.toString())){
                                    Iterator<ValueInfo> vIter=varInfo.iterator();
                                    boolean isFind=false;
                                    while(vIter.hasNext()){
                                        ValueInfo vi=vIter.next();
                                        if(vi.value.equals(arg.toString())&&vi.type==3){
                                            type=vi.type;
                                            tValue=vi.toValue;
                                            pos=vi.pos;
                                            lr=vi.lr;
                                            isFind=true;
                                            break;
                                        }
                                    }
                                    if(!isFind){
                                        continue;
                                    }
                                    if(!timeoutVar.contains(lV.toString())){
                                        ValueInfo valueInfo=new ValueInfo();
                                        valueInfo.v=lV;
                                        valueInfo.value=lV.toString();
                                        valueInfo.type=type;
                                        if(tValue!=null){
                                            valueInfo.toValue=tValue;
                                            valueInfo.pos=pos;
                                            valueInfo.lr=lr;
                                        }
                                        varInfo.add(valueInfo);
                                        timeoutVar.add(lV.toString());
                                        if(isDebug){
                                            System.out.println(lV.toString());
                                            System.out.println(stmt.getJavaSourceStartLineNumber()+" args "+arg);
                                            System.out.println(stmt.getJavaSourceStartLineNumber()+" : staticinvoke LV = timeoutVar type="+type+":"+stmt);
                                        }
                                    }else{
                                        Iterator<ValueInfo> viIter=varInfo.iterator();
                                        while(viIter.hasNext()){
                                            ValueInfo vi=viIter.next();
                                            if(vi.value.equals(lV.toString())){
                                                vi.type=type;
                                                if(tValue!=null){
                                                    vi.toValue=tValue;
                                                    vi.pos=pos;
                                                    vi.lr=lr;
                                                }
                                                if(isDebug){
                                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" args "+arg);
                                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : staticinvoke LV = timeoutVar type="+type+":"+stmt);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(stmt instanceof IfStmt){
                IfStmt ifStmt=(IfStmt)stmt;
                Value conditionValue=ifStmt.getCondition();
                Value lo=conditionValue.getUseBoxes().get(0).getValue();
                Value ro=conditionValue.getUseBoxes().get(1).getValue();
                if(isDebug){
                    System.out.println("ifStmt conditionValue:"+conditionValue);
                    // System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP toValue:"+stmt);
                }
                if((timeoutVar.contains(lo.toString())&&!timeoutVar.contains(ro.toString()))){
                    Iterator<ValueInfo> viIter=varInfo.iterator();
                    while(viIter.hasNext()){
                        ValueInfo vi=viIter.next();
                        if(vi.value.equals(lo.toString())&&vi.type==2){
                            InjectInfo info=new InjectInfo();
                            info.lr=2;
                            info.pos=stmt.getJavaSourceStartLineNumber();
                            info.cmpPos=stmt.getJavaSourceStartLineNumber();
                            info.traceType=2;
                            if(!isDuplicated(info, res)){
                                res.add(info);
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP toValue:"+stmt);
                                }
                            }
                            // type=2;
                        }else if(vi.value.equals(lo.toString())&&vi.type==3){
                            // ignore record some information (not our target)
                            // if(ro.toString().equals("0")||ro.toString().equals("0L")){
                            //     System.out.println("exclude: ro:"+ro+" method:"+sootMethod.getSubSignature());
                            //     break;
                            // }else{
                            //     System.out.println("failed: ro:"+ro);
                            // }
                            InjectInfo info=new InjectInfo();
                            info.lr=vi.lr;
                            info.pos=vi.pos;
                            info.cmpPos=stmt.getJavaSourceStartLineNumber();
                            info.traceType=1;
                            if(!isDuplicated(info, res)){
                                res.add(info);
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP toValue:"+stmt);
                                }
                            }
                        }
                    }
                }else if((!timeoutVar.contains(lo.toString())&&timeoutVar.contains(ro.toString()))){
                    Iterator<ValueInfo> viIter=varInfo.iterator();
                    while(viIter.hasNext()){
                        ValueInfo vi=viIter.next();
                        if(vi.value.equals(ro.toString())&&vi.type==2){
                            InjectInfo info=new InjectInfo();
                            info.lr=1;
                            info.pos=stmt.getJavaSourceStartLineNumber();
                            info.pos=stmt.getJavaSourceStartLineNumber();
                            info.traceType=2;
                            if(!isDuplicated(info, res)){
                                res.add(info);
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : toValue CMP timeoutVar:"+stmt);
                                }
                            }
                            // type=2;
                        }else if(vi.value.equals(lo.toString())&&vi.type==3){
                            // if(lo.toString().equals("0")||lo.toString().equals("0L")){
                            //     System.out.println("exclude: lo:"+lo+" method:"+sootMethod.getSubSignature());
                            //     break;
                            // }else{
                            //     System.out.println("failed: lo:"+lo);
                            // }
                            InjectInfo info=new InjectInfo();
                            info.lr=vi.lr;
                            info.pos=vi.pos;
                            info.cmpPos=stmt.getJavaSourceStartLineNumber();
                            info.traceType=1;
                            if(!isDuplicated(info, res)){
                                res.add(info);
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : toValue CMP timeoutVar:"+stmt);
                                }
                            }
                        }
                    }
                }else if(timeoutVar.contains(lo.toString())&&timeoutVar.contains(ro.toString())){
                    Iterator<ValueInfo> viIter=varInfo.iterator();
                    while(viIter.hasNext()){
                        ValueInfo vi=viIter.next();
                        if(vi.value.equals(lo.toString())&&vi.type==3){
                            if(vi.toValue==null){
                                System.out.println("ParseTimeOut | ---error---");
                                System.exit(0);
                            }
                            InjectInfo info=new InjectInfo();
                            info.lr=vi.lr;
                            info.pos=vi.pos;
                            info.cmpPos=stmt.getJavaSourceStartLineNumber();
                            info.traceType=1;
                            if(!isDuplicated(info, res)){
                                res.add(info);
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP timeoutVar:"+stmt);
                                }
                            }
                        }
                        if(vi.value.equals(ro.toString())&&vi.type==3){
                            if(vi.toValue==null){
                                System.out.println("ParseTimeOut.java | ---error---");
                                System.exit(0);
                            }
                            InjectInfo info=new InjectInfo();
                            info.lr=vi.lr;
                            info.pos=vi.pos;
                            info.cmpPos=stmt.getJavaSourceStartLineNumber();
                            info.traceType=1;
                            if(!isDuplicated(info, res)){
                                res.add(info);
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+" : timeoutVar CMP timeoutVar:"+stmt);
                                }
                            }
                        }
                    }
                }else{
                    continue;
                }
            }
        }
        return res;
    }
    
    public Map<SootMethod,Map<SootMethod,List<SootMethod>>> parseGeneralMethod(Map<SootMethod,List<TimeOutPoint>> timeout2Info){
        Map<SootMethod,Map<SootMethod,List<SootMethod>>> res=new LinkedHashMap<>();
        List<SootMethod> timeoutMethodList=new ArrayList<>();
        for(Map.Entry<SootMethod,List<TimeOutPoint>>entry:timeout2Info.entrySet()){
            SootMethod timeoutMethod=entry.getKey();
            List<TimeOutPoint> toPList=entry.getValue();
            Iterator<TimeOutPoint> toPIter=toPList.iterator();
            while(toPIter.hasNext()){
                TimeOutPoint toP=toPIter.next();
                if(toP.timeoutMethodType==3&&!timeoutMethodList.contains(timeoutMethod)){
                    timeoutMethodList.add(timeoutMethod);
                    break;
                }
            }
        }
        System.out.println("ParseTimeOut | generalTimeOutMethod:"+timeoutMethodList.size());
        
        // for(Map.Entry<String,Map<Integer,SootMethod>>entry:timeoutField2PosM.entrySet()){
        //     System.out.println("ParseTimeOut | field:"+entry.getKey());
        //     Map<Integer,SootMethod> pos2M=entry.getValue();
        //     for(Map.Entry<Integer,SootMethod>entry2:pos2M.entrySet()){
        //         System.out.println("ParseTimeOut | method:"+entry2.getValue());
        //     }
        // }
        System.out.println("ParseTimeOut | timeoutFieldObj:"+timeoutFieldObj.size());
        Map<SootMethod,Map<SootMethod,List<SootMethod>>> timeoutMethod2Scope=new LinkedHashMap<>();
        
        Iterator<SootMethod> timeoutIter=timeoutMethodList.iterator();
        while(timeoutIter.hasNext()){
            SootMethod timeoutMethod=timeoutIter.next();
            Map<Map<Integer,String>,Map<Integer,String>> scope=parseTimeOutFieldAndTmp(timeoutMethod,timeoutFieldObj);
            if(scope.size()!=0){
                // System.out.println("ParseTimeOut | scope!=null:"+scope);
                // System.out.println("ParseTimeOut | timeoutMethod:"+timeoutMethod);
                SootMethod endMethod=timeoutMethod;
                List<SootMethod> startList=new ArrayList<>();
                for(Map.Entry<Map<Integer,String>,Map<Integer,String>>entry:scope.entrySet()){
                    Map<Integer,String> startP=entry.getKey();
                    for(Map.Entry<Integer,String>entry2:startP.entrySet()){
                        // int startPos=entry2.getKey();
                        String field=entry2.getValue();
                        // System.out.println("ParseTimeOut | field:"+field);
                        Map<Integer,SootMethod>pos2Method=timeoutField2PosM.get(field);
                        if(pos2Method!=null){
                            for(Map.Entry<Integer,SootMethod>entry3:pos2Method.entrySet()){
                                SootMethod startMethod=entry3.getValue();
                                // System.out.println("ParseTimeOut | startmethod:"+startMethod);
                                if(!startList.contains(startMethod)){
                                    startList.add(startMethod);
                                }
                            }
                        }else{
                            if(!startList.contains(timeoutMethod)){
                                startList.add(timeoutMethod);
                            }
                        }
                    }
                }
                if(startList.size()!=0){
                    Map<SootMethod,List<SootMethod>> tmp=new LinkedHashMap<>();
                    tmp.put(endMethod, startList);
                    timeoutMethod2Scope.put(timeoutMethod,tmp);
                }

            }
        }
        res.putAll(timeoutMethod2Scope);
        return res;
    }
    /*
     * ParseTimeOutField
     */
    public Map<String,Map<Integer,SootMethod>> parseTimeOutField(SootMethod method){
        Map<String,Map<Integer,SootMethod>> res=new LinkedHashMap<>();
        Map<String,String> rl2F=new LinkedHashMap<>();
        // Map<String,String> LR=new LinkedHashMap<>();
        boolean isInvoked=false;
        List<String> invokedList=new ArrayList<>();
        Map<String,Integer> invoke2Pos=new LinkedHashMap<>();
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                int pos=stmt.getJavaSourceStartLineNumber();
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    if(invokeExpr.getMethod().getSubSignature().equals("long currentWallTime()")||
                        invokeExpr.getMethod().getSubSignature().equals("long currentElapsedTime()")){
                        isInvoked=true;
                    }
                }
                if(stmt instanceof AssignStmt){
                    AssignStmt assignStmt=(AssignStmt)stmt;
                    Value lV=assignStmt.getLeftOp();
                    Value rV=assignStmt.getRightOp();
                    if(stmt.containsFieldRef()){
                        FieldRef fieldRef=stmt.getFieldRef();
                        SootField field=fieldRef.getField();
                        if(rV.toString().contains(field.getSignature())){
                            // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                            rl2F.put(lV.toString(),field.getSignature());
                        }else{
                            // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                            rl2F.put(rV.toString(),field.getSignature());
                        }
                    }
                }
                if(stmt instanceof AssignStmt){
                    AssignStmt assignStmt=(AssignStmt)stmt;
                    Value lV=assignStmt.getLeftOp();
                    Value rV=assignStmt.getRightOp();
                    if(isInvoked){
                        if(!invokedList.contains(lV.toString())){
                            invokedList.add(lV.toString());
                            invoke2Pos.put(lV.toString(), pos);
                            // System.out.println("aaaa"+stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                        }
                        isInvoked=false;
                    }else{
                        if((rV instanceof DivExpr)||(rV instanceof MulExpr)){
                            Value lo=null;
                            Value ro=null;
                            if(rV instanceof DivExpr){
                                DivExpr expr=(DivExpr)rV;
                                lo=expr.getOp1();
                                ro=expr.getOp2();
                                if(invokedList.contains(lo.toString())||invokedList.contains(ro.toString())){
                                    if(!invokedList.contains(lV.toString())){
                                        invokedList.add(lV.toString());
                                        // System.out.println("aaaa"+stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                                    }
                                }
                                if(invokedList.contains(lo.toString())){
                                    // LR.put(lV.toString(), lo.toString());
                                    int invokePos=invoke2Pos.get(lo.toString());
                                    invoke2Pos.put(lV.toString(), invokePos);

                                }else if(invokedList.contains(ro.toString())){
                                    // LR.put(lV.toString(), ro.toString());
                                    int invokePos=invoke2Pos.get(ro.toString());
                                    invoke2Pos.put(lV.toString(), invokePos);
                                }
                            }else{
                                MulExpr expr=(MulExpr)rV;
                                lo=expr.getOp1();
                                ro=expr.getOp2();
                                if(invokedList.contains(lo.toString())||invokedList.contains(ro.toString())){
                                    if(!invokedList.contains(lV.toString())){
                                        invokedList.add(lV.toString());
                                        // System.out.println("aaaa"+stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                                    }
                                }
                                if(invokedList.contains(lo.toString())){
                                    // LR.put(lV.toString(), lo.toString());
                                    int invokePos=invoke2Pos.get(lo.toString());
                                    invoke2Pos.put(lV.toString(), invokePos);

                                }else if(invokedList.contains(ro.toString())){
                                    // LR.put(lV.toString(), ro.toString());
                                    int invokePos=invoke2Pos.get(ro.toString());
                                    invoke2Pos.put(lV.toString(), invokePos);

                                }
                            }
                        }else if((rV instanceof AddExpr)||(rV instanceof SubExpr)){
                            Value lo=null;
                            Value ro=null;
                            if(rV instanceof AddExpr){
                                AddExpr expr=(AddExpr)rV;
                                lo=expr.getOp1();
                                ro=expr.getOp2();
                            }else{
                                SubExpr expr=(SubExpr)rV;
                                lo=expr.getOp1();
                                ro=expr.getOp2();
                            }
                            if(invokedList.contains(lo.toString())||invokedList.contains(ro.toString())){
                                if(!invokedList.contains(lV.toString())){
                                    invokedList.add(lV.toString());
                                    // System.out.println("aaaa"+stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                                }
                            }
                            if(invokedList.contains(lo.toString())){
                                // LR.put(lV.toString(), lo.toString());
                                int invokePos=invoke2Pos.get(lo.toString());
                                invoke2Pos.put(lV.toString(), invokePos);

                            }
                            if(invokedList.contains(ro.toString())){
                                // LR.put(lV.toString(), ro.toString());
                                int invokePos=invoke2Pos.get(ro.toString());
                                invoke2Pos.put(lV.toString(), invokePos);

                            }
                        }else{
                            if(invokedList.contains(rV.toString())){
                                if(!invokedList.contains(lV.toString())){
                                    invokedList.add(lV.toString());
                                    // System.out.println("aaaa"+stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                                    int invokePos=invoke2Pos.get(rV.toString());
                                    invoke2Pos.put(lV.toString(), invokePos);
                                }
                                // LR.put(lV.toString(), rV.toString());
                            }
                        }
                    }
                }
            }
            for(Map.Entry<String,String>entry:rl2F.entrySet()){
                String value=entry.getKey();
                String field=entry.getValue();
                Map<Integer,SootMethod> temp=new LinkedHashMap<>();
                for(Map.Entry<String,Integer>entry2:invoke2Pos.entrySet()){
                    String invokeValue=entry2.getKey();
                    int pos=entry2.getValue();
                    if(invokeValue.equals(value)){
                        // System.out.println("pos:"+pos);
                        temp.put(pos, method);
                    }
                }
                if(temp.size()!=0){
                    // System.out.println("value:"+value+" field:"+field);
                    res.put(field, temp);
                }
            }
        }

        return res;
    }
    
    public Map<Map<Integer,String>,Map<Integer,String>> parseTimeOutFieldAndTmp(SootMethod method,List<String>timeoutFieldList){
        Map<Map<Integer,String>,Map<Integer,String>> scope=new LinkedHashMap<>();
        Map<Map<Integer,String>,Map<Integer,String>> tmp=new LinkedHashMap<>();
        List<Value> timeoutVar=new ArrayList<>();
        List<ValueInfo> varInfo=new ArrayList<>();
        int startPos=-5;
        String startVar=null;
        int endPos=-5;
        String endVar=null;
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                startPos=-5;
                startVar=null;
                endPos=-5;
                endVar=null;
                if(stmt instanceof AssignStmt){
                    AssignStmt assignStmt=(AssignStmt)stmt;
                    Value lV=assignStmt.getLeftOp();
                    Value rV=assignStmt.getRightOp();
                    if(stmt.containsFieldRef()){
                        FieldRef fieldRef=stmt.getFieldRef();
                        SootField field=fieldRef.getField();
                        if(timeoutFieldList.contains(field.getSignature())){
                            if(rV.toString().contains(field.getSignature())){
                                if(!timeoutVar.contains(lV)){
                                    timeoutVar.add(lV);
                                    ValueInfo valueInfo=new ValueInfo();
                                    valueInfo.type=0;
                                    valueInfo.value=lV.toString();
                                    valueInfo.v=lV;
                                    valueInfo.pos=stmt.getJavaSourceStartLineNumber();
                                    valueInfo.timeoutPos=-2;
                                    valueInfo.timeoutValue=field.getSignature();
                                    varInfo.add(valueInfo);
                                }
                            }
                        }
                    }
                }
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    if(invokeExpr.getMethod().getSubSignature().equals("long currentWallTime()")||
                        invokeExpr.getMethod().getSubSignature().equals("long currentElapsedTime()")){
                        List<ValueBox> defBoxes=stmt.getDefBoxes();
                        Iterator<ValueBox> defIter=defBoxes.iterator();
                        while(defIter.hasNext()){
                            ValueBox vb=defIter.next();
                            if(!timeoutVar.contains(vb.getValue())){
                                timeoutVar.add(vb.getValue());
                                ValueInfo valueInfo=new ValueInfo();
                                valueInfo.v=vb.getValue();
                                valueInfo.type=0;
                                valueInfo.pos=stmt.getJavaSourceStartLineNumber();
                                valueInfo.timeoutPos=stmt.getJavaSourceStartLineNumber();
                                valueInfo.timeoutValue=vb.getValue().toString();
                                valueInfo.value=vb.getValue().toString();
                                varInfo.add(valueInfo);
                            }else{
                                Iterator<ValueInfo> vIter=varInfo.iterator();
                                while(vIter.hasNext()){
                                    ValueInfo vi=vIter.next();
                                    if(vi.v.equals(vb.getValue())){
                                        vi.type=0;
                                        break;
                                    }
                                }
                            }
                        }   
                    }
                }

                if(stmt instanceof AssignStmt){
                    AssignStmt assignStmt=(AssignStmt)stmt;
                    Value lV=assignStmt.getLeftOp();
                    Value rV=assignStmt.getRightOp();
                    if(rV instanceof CmpExpr){
                        // System.out.println("Cmp:"+stmt);
                        CmpExpr ce=(CmpExpr)rV;
                        Value lo=ce.getOp1();
                        Value ro=ce.getOp2();
                        if((timeoutVar.contains(lo)&&!timeoutVar.contains(ro))){
                            Iterator<ValueInfo> viIter=varInfo.iterator();
                            while(viIter.hasNext()){
                                ValueInfo vi=viIter.next();
                                // 
                                if(vi.v.equals(lo)&&(vi.type==2||vi.type==3)){
                                    startPos=vi.lTimeOutPos;
                                    startVar=vi.lTimeOutValue;
                                    endPos=vi.rTimeOutPos;
                                    endVar=vi.rTimeOutValue;
                                    Map<Integer,String>tmp1=new LinkedHashMap<>();
                                    Map<Integer,String>tmp2=new LinkedHashMap<>();
                                    tmp1.put(startPos, startVar);
                                    tmp2.put(endPos, endVar);
                                    tmp.put(tmp1,tmp2);
                                }else if(vi.v.equals(lo)&&vi.type==3){

                                }
                            }
                        }else if((!timeoutVar.contains(lo)&&timeoutVar.contains(ro))){
                            Iterator<ValueInfo> viIter=varInfo.iterator();
                            while(viIter.hasNext()){
                                ValueInfo vi=viIter.next();
                                if(vi.v.equals(ro)&&(vi.type==2||vi.type==3)){
                                    startPos=vi.lTimeOutPos;
                                    startVar=vi.lTimeOutValue;
                                    endPos=vi.rTimeOutPos;
                                    endVar=vi.rTimeOutValue;
                                    Map<Integer,String>tmp1=new LinkedHashMap<>();
                                    Map<Integer,String>tmp2=new LinkedHashMap<>();
                                    tmp1.put(startPos, startVar);
                                    tmp2.put(endPos, endVar);
                                    tmp.put(tmp1,tmp2);
                                }
                            }
                        }else if(timeoutVar.contains(lo)&&timeoutVar.contains(ro)){
                            ValueInfo lValueInfo=null;
                            ValueInfo rValueInfo=null;
                            Iterator<ValueInfo> viIter=varInfo.iterator();
                            while(viIter.hasNext()){
                                ValueInfo vi=viIter.next();
                                if(vi.v.equals(lo)){
                                    lValueInfo=vi;
                                }
                                if(vi.v.equals(ro)){
                                    rValueInfo=vi;
                                }
                            }
                            if(lValueInfo.type==3){
                                if(rValueInfo.type==0||rValueInfo.type==1){
                                    startPos=lValueInfo.timeoutPos;
                                    startVar=lValueInfo.timeoutValue;
                                    endPos=rValueInfo.timeoutPos;
                                    endVar=rValueInfo.timeoutValue;
                                    Map<Integer,String>tmp1=new LinkedHashMap<>();
                                    Map<Integer,String>tmp2=new LinkedHashMap<>();
                                    tmp1.put(startPos, startVar);
                                    tmp2.put(endPos, endVar);
                                    tmp.put(tmp1,tmp2);
                                }
                            }else{
                                if(rValueInfo.type==3){
                                    if(lValueInfo.type==0||lValueInfo.type==1){
                                        startPos=lValueInfo.timeoutPos;
                                        startVar=lValueInfo.timeoutValue;
                                        endPos=rValueInfo.timeoutPos;
                                        endVar=rValueInfo.timeoutValue;
                                        Map<Integer,String>tmp1=new LinkedHashMap<>();
                                        Map<Integer,String>tmp2=new LinkedHashMap<>();
                                        tmp1.put(startPos, startVar);
                                        tmp2.put(endPos, endVar);
                                        tmp.put(tmp1,tmp2);
                                    }
                                }
                            }
                        }else{
                            continue;
                        }
                    }else if((rV instanceof DivExpr)||(rV instanceof MulExpr)){
                        // System.out.println("Div|Mul"+stmt);
                        Value lo,ro;
                        int type=0;
                        int timeoutPos=-1;
                        String timeoutValue=null;
                        int lTimeoutPos=-1;
                        String lTimeoutValue=null;
                        int rTimeoutPos=-1;
                        String rTimeoutValue=null;
                        if(rV instanceof DivExpr){
                            DivExpr divExpr=(DivExpr)rV;
                            lo=divExpr.getOp1();
                            ro=divExpr.getOp2();
                        }else{
                            MulExpr mulExpr=(MulExpr)rV;
                            lo=mulExpr.getOp1();
                            ro=mulExpr.getOp2();
                        }
                        if(timeoutVar.contains(lo)){
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.v.equals(lo)){
                                    if(vi.type==0){
                                        type=1;
                                    }else{
                                        type=vi.type;
                                    }
                                    timeoutPos=vi.timeoutPos;
                                    timeoutValue=vi.timeoutValue;
                                    lTimeoutPos=vi.lTimeOutPos;
                                    lTimeoutValue=vi.lTimeOutValue;
                                    rTimeoutPos=vi.rTimeOutPos;
                                    rTimeoutValue=vi.rTimeOutValue;
                                    break;
                                }
                            }
                        }else if(timeoutVar.contains(ro)){
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.v.equals(ro)){
                                    if(vi.type==0){
                                        type=1;
                                    }else{
                                        type=vi.type;
                                    }
                                    timeoutPos=vi.timeoutPos;
                                    timeoutValue=vi.timeoutValue;
                                    lTimeoutPos=vi.lTimeOutPos;
                                    lTimeoutValue=vi.lTimeOutValue;
                                    rTimeoutPos=vi.rTimeOutPos;
                                    rTimeoutValue=vi.rTimeOutValue;
                                    break;
                                }
                            }
                        }else{
                            continue;
                        }
                        if(timeoutVar.contains(lV)){
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.v.equals(lV)){
                                    vi.type=type;
                                    vi.timeoutPos=timeoutPos;
                                    vi.timeoutValue=timeoutValue;
                                    vi.lTimeOutPos=lTimeoutPos;
                                    vi.lTimeOutValue=lTimeoutValue;
                                    vi.rTimeOutPos=rTimeoutPos;
                                    vi.rTimeOutValue=rTimeoutValue;
                                    break;
                                }
                            }
                        }else{
                            timeoutVar.add(lV);
                            ValueInfo valueInfo=new ValueInfo();
                            valueInfo.v=lV;
                            valueInfo.type=type;
                            valueInfo.pos=stmt.getJavaSourceStartLineNumber();
                            valueInfo.timeoutPos=timeoutPos;
                            valueInfo.timeoutValue=timeoutValue;
                            valueInfo.lTimeOutPos=lTimeoutPos;
                            valueInfo.lTimeOutValue=lTimeoutValue;
                            valueInfo.rTimeOutValue=lTimeoutValue;
                            valueInfo.rTimeOutPos=lTimeoutPos;
                            valueInfo.value=lV.toString();
                            varInfo.add(valueInfo);
                        }
                    }else if((rV instanceof AddExpr)||(rV instanceof SubExpr)){
                        // System.out.println("Add|Sub:"+stmt);
                        Value lo,ro;
                        Value tValue=null;
                        int type=0;
                        int lr=0;
                        int timeoutPos=-1;
                        String timeoutValue=null;
                        int lTimeoutPos=-1;
                        String lTimeoutValue=null;
                        int rTimeoutPos=-1;
                        String rTimeoutValue=null;
                        boolean isValueSubType2=false;
                        if(rV instanceof AddExpr){
                            AddExpr addExpr=(AddExpr)rV;
                            lo=addExpr.getOp1();
                            ro=addExpr.getOp2();
                        }else{
                            SubExpr subExpr=(SubExpr)rV;
                            lo=subExpr.getOp1();
                            ro=subExpr.getOp2();
                        }
                        if(timeoutVar.contains(lo)&&timeoutVar.contains(ro)&&(rV instanceof SubExpr)){
                            type=2;
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.v.equals(lo)){
                                    lTimeoutPos=vi.timeoutPos;
                                    lTimeoutValue=vi.timeoutValue;
                                }else if(vi.v.equals(ro)){
                                    rTimeoutPos=vi.timeoutPos;
                                    rTimeoutValue=vi.timeoutValue;
                                }
                            }
                        }else if(timeoutVar.contains(lo)&&!timeoutVar.contains(ro)){
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.v.equals(lo)&&vi.type==2){
                                    lTimeoutPos=vi.lTimeOutPos;
                                    lTimeoutValue=vi.lTimeOutValue;
                                    rTimeoutPos=vi.rTimeOutPos;
                                    rTimeoutValue=vi.rTimeOutValue;
                                    isValueSubType2=true;
                                    break;
                                }else if(vi.v.equals(lo)){
                                    timeoutPos=vi.timeoutPos;
                                    timeoutValue=vi.timeoutValue;
                                    break;
                                }
                            }
                            type=3;
                            tValue=ro;
                            lr=2;
                        }else if(!timeoutVar.contains(lo)&&timeoutVar.contains(ro)){
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.v.equals(ro)&&vi.type==2){
                                    lTimeoutPos=vi.lTimeOutPos;
                                    lTimeoutValue=vi.lTimeOutValue;
                                    rTimeoutPos=vi.rTimeOutPos;
                                    rTimeoutValue=vi.rTimeOutValue;
                                    isValueSubType2=true;  
                                    break;
                                }else if(vi.v.equals(ro)){
                                    timeoutPos=vi.timeoutPos;
                                    timeoutValue=vi.timeoutValue;
                                    break;
                                }
                            }
                            type=3;
                            tValue=lo;
                            lr=1;
                        }else{
                            continue;
                        }
                        if(!timeoutVar.contains(lV)){
                            ValueInfo valueInfo=new ValueInfo();
                            valueInfo.v=lV;
                            valueInfo.value=lV.toString();
                            valueInfo.type=type;
                            if(type==2||isValueSubType2){
                                valueInfo.lTimeOutPos=lTimeoutPos;
                                valueInfo.lTimeOutValue=lTimeoutValue;
                                valueInfo.rTimeOutPos=rTimeoutPos;
                                valueInfo.rTimeOutValue=rTimeoutValue;
                            }else{
                                valueInfo.timeoutPos=timeoutPos;
                                valueInfo.timeoutValue=timeoutValue;
                            }
                            if(tValue!=null){
                                valueInfo.toValue=tValue;
                                valueInfo.pos=stmt.getJavaSourceStartLineNumber();
                                valueInfo.lr=lr;
                            }
                            varInfo.add(valueInfo);
                            timeoutVar.add(lV);
                        }else{
                            Iterator<ValueInfo> vIter=varInfo.iterator();
                            while(vIter.hasNext()){
                                ValueInfo vi=vIter.next();
                                if(vi.v.equals(lV)){
                                    vi.type=type;
                                    if(type==2||isValueSubType2){
                                        vi.lTimeOutPos=lTimeoutPos;
                                        vi.lTimeOutValue=lTimeoutValue;
                                        vi.rTimeOutPos=rTimeoutPos;
                                        vi.rTimeOutValue=rTimeoutValue;
                                    }else{
                                        vi.timeoutPos=timeoutPos;
                                        vi.timeoutValue=timeoutValue;
                                    }
                                    if(tValue!=null){
                                        vi.toValue=tValue;
                                        vi.pos=stmt.getJavaSourceStartLineNumber();
                                        vi.lr=lr;
                                    }
                                    break;
                                }
                            }
                        }
                    }else{
                        List<ValueBox> useBoxes=rV.getUseBoxes();
                        if(useBoxes.size()==0||useBoxes.size()==1){
                            Value temp;
                            if(useBoxes.size()==0){
                                temp=rV;
                            }else{
                                temp=useBoxes.get(0).getValue();
                            }
                            // System.out.println("stmt:"+stmt);
                            int type=0;
                            Value tValue=null;
                            int pos=0;
                            int lr=0;
                            int timeoutPos=-1;
                            String timeoutValue=null;
                            int lTimeoutPos=-1;
                            String lTimeoutValue=null;
                            int rTimeoutPos=-1;
                            String rTimeoutValue=null;
                            if(timeoutVar.contains(temp)){
                                Iterator<ValueInfo> vIter=varInfo.iterator();
                                boolean isFind=false;
                                while(vIter.hasNext()){
                                    ValueInfo vi=vIter.next();
                                    if(vi.v.equals(temp)){
                                        type=vi.type;
                                        tValue=vi.toValue;
                                        pos=vi.pos;
                                        lr=vi.lr;
                                        timeoutPos=vi.timeoutPos;
                                        timeoutValue=vi.timeoutValue;
                                        lTimeoutPos=vi.lTimeOutPos;
                                        lTimeoutValue=vi.lTimeOutValue;
                                        rTimeoutPos=vi.rTimeOutPos;
                                        rTimeoutValue=vi.rTimeOutValue;
                                        isFind=true;
                                        break;
                                    }
                                }
                                if(!isFind){
                                    continue;
                                }
                                if(!timeoutVar.contains(lV)){
                                    ValueInfo valueInfo=new ValueInfo();
                                    valueInfo.v=lV;
                                    valueInfo.value=lV.toString();
                                    valueInfo.type=type;
                                    valueInfo.lTimeOutPos=lTimeoutPos;
                                    valueInfo.lTimeOutValue=lTimeoutValue;
                                    valueInfo.rTimeOutPos=rTimeoutPos;
                                    valueInfo.rTimeOutValue=rTimeoutValue;
                                    valueInfo.timeoutPos=timeoutPos;
                                    valueInfo.timeoutValue=timeoutValue;
                                    if(tValue!=null){
                                        valueInfo.toValue=tValue;
                                        valueInfo.pos=pos;
                                        valueInfo.lr=lr;
                                    }
                                    varInfo.add(valueInfo);
                                    timeoutVar.add(lV);
                                }else{
                                    Iterator<ValueInfo> viIter=varInfo.iterator();
                                    while(viIter.hasNext()){
                                        ValueInfo vi=viIter.next();
                                        if(vi.v.equals(lV)){
                                            vi.type=type;
                                            vi.lTimeOutPos=lTimeoutPos;
                                            vi.lTimeOutValue=lTimeoutValue;
                                            vi.rTimeOutPos=rTimeoutPos;
                                            vi.rTimeOutValue=rTimeoutValue;
                                            vi.timeoutPos=timeoutPos;
                                            vi.timeoutValue=timeoutValue;
                                            if(tValue!=null){
                                                vi.toValue=tValue;
                                                vi.pos=pos;
                                                vi.lr=lr;      
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }else{
                            if(rV instanceof StaticInvokeExpr){
                                int type=0;
                                Value tValue=null;
                                int pos=0;
                                int lr=0;
                                int timeoutPos=-1;
                                String timeoutValue=null;
                                int lTimeoutPos=-1;
                                String lTimeoutValue=null;
                                int rTimeoutPos=-1;
                                String rTimeoutValue=null;
                                StaticInvokeExpr staticInvokeExpr=(StaticInvokeExpr)rV;
                                List<Value> args=staticInvokeExpr.getArgs();
                                Iterator<Value> argIter=args.iterator();
                                while(argIter.hasNext()){
                                    Value arg=argIter.next();
                                    if(timeoutVar.contains(arg)){
                                        Iterator<ValueInfo> vIter=varInfo.iterator();
                                        boolean isFind=false;
                                        while(vIter.hasNext()){
                                            ValueInfo vi=vIter.next();
                                            if(vi.v.equals(arg)&&vi.type==3){
                                                type=vi.type;
                                                tValue=vi.toValue;
                                                pos=vi.pos;
                                                lr=vi.lr;
                                                timeoutPos=vi.timeoutPos;
                                                timeoutValue=vi.timeoutValue;
                                                lTimeoutPos=vi.lTimeOutPos;
                                                lTimeoutValue=vi.lTimeOutValue;
                                                rTimeoutPos=vi.rTimeOutPos;
                                                rTimeoutValue=vi.rTimeOutValue;
                                                isFind=true;
                                                break;
                                            }
                                        }
                                        if(!isFind){
                                            continue;
                                        }
                                        if(!timeoutVar.contains(lV)){
                                            ValueInfo valueInfo=new ValueInfo();
                                            valueInfo.v=lV;
                                            valueInfo.value=lV.toString();
                                            valueInfo.type=type;
                                            valueInfo.lTimeOutPos=lTimeoutPos;
                                            valueInfo.lTimeOutValue=lTimeoutValue;
                                            valueInfo.rTimeOutPos=rTimeoutPos;
                                            valueInfo.rTimeOutValue=rTimeoutValue;
                                            valueInfo.timeoutPos=timeoutPos;
                                            valueInfo.timeoutValue=timeoutValue;
                                            if(tValue!=null){
                                                valueInfo.toValue=tValue;
                                                valueInfo.pos=pos;
                                                valueInfo.lr=lr;
                                            }
                                            varInfo.add(valueInfo);
                                            timeoutVar.add(lV);
                                        }else{
                                            Iterator<ValueInfo> viIter=varInfo.iterator();
                                            while(viIter.hasNext()){
                                                ValueInfo vi=viIter.next();
                                                if(vi.v.equals(lV)){
                                                    vi.type=type;
                                                    vi.lTimeOutPos=lTimeoutPos;
                                                    vi.lTimeOutValue=lTimeoutValue;
                                                    vi.rTimeOutPos=rTimeoutPos;
                                                    vi.rTimeOutValue=rTimeoutValue;
                                                    vi.timeoutPos=timeoutPos;
                                                    vi.timeoutValue=timeoutValue;
                                                    if(tValue!=null){
                                                        vi.toValue=tValue;
                                                        vi.pos=pos;
                                                        vi.lr=lr;      
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if(stmt instanceof IfStmt){
                    IfStmt ifStmt=(IfStmt)stmt;
                    Value conValue=ifStmt.getCondition();
                    Value lo=conValue.getUseBoxes().get(0).getValue();
                    Value ro=conValue.getUseBoxes().get(1).getValue();
                    if((timeoutVar.contains(lo)&&!timeoutVar.contains(ro))){
                        Iterator<ValueInfo> viIter=varInfo.iterator();
                        while(viIter.hasNext()){
                            ValueInfo vi=viIter.next();
                            if(vi.v.equals(lo)&&(vi.type==2||vi.type==3)){
                                startPos=vi.lTimeOutPos;
                                startVar=vi.lTimeOutValue;
                                endPos=vi.rTimeOutPos;
                                endVar=vi.rTimeOutValue;
                                Map<Integer,String>tmp1=new LinkedHashMap<>();
                                Map<Integer,String>tmp2=new LinkedHashMap<>();
                                tmp1.put(startPos, startVar);
                                tmp2.put(endPos, endVar);
                                tmp.put(tmp1,tmp2);
                            }
                        }
                    }else if((!timeoutVar.contains(lo)&&timeoutVar.contains(ro))){
                        Iterator<ValueInfo> viIter=varInfo.iterator();
                        while(viIter.hasNext()){
                            ValueInfo vi=viIter.next();
                            if(vi.v.equals(ro)&&(vi.type==2||vi.type==3)){
                                startPos=vi.lTimeOutPos;
                                startVar=vi.lTimeOutValue;
                                endPos=vi.rTimeOutPos;
                                endVar=vi.rTimeOutValue;
                                Map<Integer,String>tmp1=new LinkedHashMap<>();
                                Map<Integer,String>tmp2=new LinkedHashMap<>();
                                tmp1.put(startPos, startVar);
                                tmp2.put(endPos, endVar);
                                tmp.put(tmp1,tmp2);
                            }
                        }
                    }else if(timeoutVar.contains(lo)&&timeoutVar.contains(ro)){
                       ValueInfo lValueInfo=null;
                       ValueInfo rValueInfo=null;
                       Iterator<ValueInfo> viIter=varInfo.iterator();
                       while(viIter.hasNext()){
                           ValueInfo vi=viIter.next();
                           if(vi.value.equals(lo.toString())){
                               lValueInfo=vi;
                           }
                           if(vi.value.equals(ro.toString())){
                               rValueInfo=vi;
                           }
                       }
                       if(lValueInfo.type==3){
                           if(rValueInfo.type==0||rValueInfo.type==1){
                               startPos=lValueInfo.timeoutPos;
                               startVar=lValueInfo.timeoutValue;
                               endPos=rValueInfo.timeoutPos;
                               endVar=rValueInfo.timeoutValue;
                               Map<Integer,String>tmp1=new LinkedHashMap<>();
                               Map<Integer,String>tmp2=new LinkedHashMap<>();
                               tmp1.put(startPos, startVar);
                               tmp2.put(endPos, endVar);
                               tmp.put(tmp1,tmp2);
                           }
                       }else{
                           if(rValueInfo.type==3){
                               if(lValueInfo.type==0||lValueInfo.type==1){
                                   startPos=lValueInfo.timeoutPos;
                                   startVar=lValueInfo.timeoutValue;
                                   endPos=rValueInfo.timeoutPos;
                                   endVar=rValueInfo.timeoutValue;
                                   Map<Integer,String>tmp1=new LinkedHashMap<>();
                                   Map<Integer,String>tmp2=new LinkedHashMap<>();
                                   tmp1.put(startPos, startVar);
                                   tmp2.put(endPos, endVar);
                                   tmp.put(tmp1,tmp2);
                               }
                           }
                       }
                    }else{
                        continue;
                    }
                }
            }
        }
        for(Map.Entry<Map<Integer,String>,Map<Integer,String>>entry:tmp.entrySet()){
            Map<Integer,String> tmp1=entry.getKey();
            Map<Integer,String> tmp2=entry.getValue();
            for(Map.Entry<Integer,String>entry1:tmp1.entrySet()){
                startPos=entry1.getKey();
                startVar=entry1.getValue();
            }
            for(Map.Entry<Integer,String>entry2:tmp2.entrySet()){
                endPos=entry2.getKey();
                endVar=entry2.getValue();
            }
            if(startPos==-5||endPos==-5){
                return null;
            }
            Map<Integer,String> startP=new LinkedHashMap<>();
            Map<Integer,String> endP=new LinkedHashMap<>();
            if(startPos==-2){
                startP.put(startPos, startVar);
                endP.put(endPos, endVar);
            }else if(endPos==-2){
                startP.put(endPos, endVar);
                endP.put(startPos, startVar);
            }else{
                if(endPos>startPos){
                    startP.put(startPos, startVar);
                    endP.put(endPos, endVar);
                }else{
                    startP.put(endPos, endVar);
                    endP.put(startPos, startVar);
                }
            }
            scope.put(startP, endP);
        }
        return scope;
    }

    public Map<SootMethod,Map<SootMethod,Integer>> parseWAT2NSRMethod(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap, Map<SootMethod,List<TimeOutPoint>> timeout2Info){
        List<SootMethod> timeoutMethodList=new ArrayList<>();
        for(Map.Entry<SootMethod,List<TimeOutPoint>>entry:timeout2Info.entrySet()){
            timeoutMethodList.add(entry.getKey());

        }
        // System.out.println("ParseTimeOut | timeoutMethodList:"+timeoutMethodList.size());
        // notify|signal|release
        List<SootMethod> nsrMethodList=new ArrayList<>();
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            for(Map.Entry<SootMethod,Integer>en:entry.getKey().entrySet()){
                if(isContainNSR(en.getKey())){
                    nsrMethodList.add(en.getKey());
                }
            }
        }
        System.out.println("ParseTimeOut | nsrMethodList:"+nsrMethodList.size());
        List<String> watObj=new ArrayList<>();
        Map<SootMethod,List<String>> wat2Obj=new LinkedHashMap<>();
        Iterator<SootMethod> toIter=timeoutMethodList.iterator();
        while(toIter.hasNext()){
            SootMethod toMethod=toIter.next();
            watObj=getWATJSObj(toMethod,1);
            if(watObj.size()!=0){
                wat2Obj.put(toMethod, watObj);
            }else{
                int type=getType(toMethod);
                if(type==2){
                    wat2Obj.put(toMethod, watObj);
                }
            }
        }
        System.out.println("ParseTimeOut | wat2Obj:"+wat2Obj.size());
        // for(Map.Entry<SootMethod,List<String>>entry:wat2Obj.entrySet()){
        //     SootMethod watMethod=entry.getKey();
        //     List<String> watObjs=entry.getValue();
            // System.out.println("watMethod:"+watMethod+" objs:"+watObjs);
        // }

        Map<SootMethod,Map<String,Integer>> nsrObjList=new LinkedHashMap<>();
        nsrObjList=getNSRObj(nsrMethodList);
        System.out.println("ParseTimeOut | nsrObjList:"+nsrObjList.size());
        // for(Map.Entry<SootMethod,Map<String,Integer>>entry:nsrObjList.entrySet()){
        //     SootMethod nsrMethod=entry.getKey();
        //     Map<String,Integer> nsrObjs=entry.getValue();
        //     for(Map.Entry<String,Integer>entry2:nsrObjs.entrySet()){
        //         String obj=entry2.getKey();
        //         int loc=entry2.getValue();
                // System.out.println("nsrMethod:"+nsrMethod+" obj:"+obj+" loc:"+loc);
        //     }
        // }
        Map<SootMethod,Map<SootMethod,Integer>> watM2nsrM=new LinkedHashMap<>();
        watM2nsrM=matchWAT2NSR(wat2Obj,nsrObjList);
        return watM2nsrM;
    }
    
    public Map<SootMethod,List<SootMethod>> parseJoinMethod(Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap, Map<SootMethod,List<TimeOutPoint>> timeout2Info,CallGraph cg){
        List<SootMethod> timeoutMethodList=new ArrayList<>();
        for(Map.Entry<SootMethod,List<TimeOutPoint>>entry:timeout2Info.entrySet()){
            timeoutMethodList.add(entry.getKey());
        }
        List<SootMethod> startMethodList=new ArrayList<>();
        Chain<SootClass>classChain=Scene.v().getClasses();
        Iterator<SootClass>cIterator=classChain.iterator();
        while(cIterator.hasNext()){
            SootClass sootClass=cIterator.next();
            if(!isSystemClass(sootClass)){
                continue;
            }
            List<SootMethod> methodChain=sootClass.getMethods();
            Iterator<SootMethod> mIterator=methodChain.iterator();
            while(mIterator.hasNext()){
                SootMethod sootMethod=mIterator.next();
                if(isContainStart(sootMethod)){
                    startMethodList.add(sootMethod);
                }
            }
        }
        List<SootMethod> runMethodList=new ArrayList<>();
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            for(Map.Entry<SootMethod,Integer>en:entry.getKey().entrySet()){
                SootMethod method=en.getKey();
                if(method.getSubSignature().equals("void run()")&&!runMethodList.contains(method)){
                    runMethodList.add(method);
                }
            }
        }
        System.out.println("ParseTimeOut | runMethodList::"+runMethodList.size());
        System.out.println("ParseTimeOut | startMethodList::"+startMethodList.size());
        List<String>joinObj=new ArrayList<>();
        Map<SootMethod,List<String>> join2Obj=new LinkedHashMap<>();
        Iterator<SootMethod> toMIter=timeoutMethodList.iterator();
        while(toMIter.hasNext()){
            SootMethod toMethod=toMIter.next();
            joinObj=getWATJSObj(toMethod,2);
            if(joinObj.size()!=0){
                // System.out.println("joinMethod:"+toMethod+" obj:"+joinObj);
                join2Obj.put(toMethod, joinObj);
            }
        }

        Map<SootMethod,List<String>>start2Obj=new LinkedHashMap<>();
        List<String> startObj=new ArrayList<>();
        Iterator<SootMethod> startIter=startMethodList.iterator();
        while(startIter.hasNext()){
            SootMethod startMethod=startIter.next();
            startObj=getWATJSObj(startMethod, 3);
            if(startObj.size()!=0){
                start2Obj.put(startMethod, startObj);
                // System.out.println("startMethod:"+startMethod);
                // System.out.println("startObj:"+startObj);
            }
        }
        System.out.println("ParseTimeOut | start2Obj:"+start2Obj.size());
        Map<SootMethod,List<SootMethod>> join2Run=new LinkedHashMap<>();
        join2Run=matchJoin2Run(join2Obj,start2Obj,runMethodList,cg);

        return join2Run;
    }
    public Map<SootMethod,List<SootMethod>> matchJoin2Run(Map<SootMethod,List<String>>join2Obj,Map<SootMethod,List<String>>start2Obj,List<SootMethod> runMethodList,CallGraph cg){
        Map<SootMethod,List<SootMethod>> res=new LinkedHashMap<>();
        // get run method in csMap
        // runMethodList

        // join->start->run
        for(Map.Entry<SootMethod,List<String>>entry:join2Obj.entrySet()){
            SootMethod joinMethod=entry.getKey();
            List<String> joinObj=entry.getValue();
            List<SootMethod> runList=new ArrayList<>();
            for(Map.Entry<SootMethod,List<String>>entry2:start2Obj.entrySet()){
                SootMethod startMethod=entry2.getKey();
                List<String> startObj=entry2.getValue();
                Iterator<String> sIter=startObj.iterator();
                while(sIter.hasNext()){
                    String sObj=sIter.next();
                    if(matchObj(joinObj, sObj)){
                        // System.out.println("matched start obj:"+sObj);
                        // System.out.println("matched start:"+startMethod);
                        TransitiveTargets transitiveTargets=new TransitiveTargets(cg);
                        Iterator<MethodOrMethodContext> callees=transitiveTargets.iterator(startMethod);
                        while(callees.hasNext()){
                            SootMethod callee=(SootMethod)callees.next();
                            if(callee.getSubSignature().equals("void run()")){
                                // System.out.println("matched start obj -> run:"+sObj);
                                // System.out.println("matched start -> run:"+startMethod);
                                if(runMethodList.contains(callee)&&!runList.contains(callee)){
                                    runList.add(callee);
                                }
                            }
                        }
                        break;
                    }
                }
            }
            if(runList.size()!=0){
                res.put(joinMethod, runList);
            }
        }
        return res;
    }

    public boolean matchObj(List<String>objList,String tgt){
        Iterator<String> objIter=objList.iterator();
        while(objIter.hasNext()){
            String obj=objIter.next();
            if(obj.contains(tgt)||tgt.contains(obj)){
                return true;
            }
        }
        return false;
    }

    public Map<SootMethod,Map<SootMethod,Integer>> matchWAT2NSR(Map<SootMethod,List<String>>watM2Obj,Map<SootMethod,Map<String,Integer>>nsrObjList){
        Map<SootMethod,Map<SootMethod,Integer>> res=new LinkedHashMap<>();
        for(Map.Entry<SootMethod,List<String>>entry:watM2Obj.entrySet()){
            SootMethod toMethod=entry.getKey();
            int type=getType(toMethod);
            List<String> objList=entry.getValue();
            Map<SootMethod,Integer> tmp=new LinkedHashMap<>();
            for(Map.Entry<SootMethod,Map<String,Integer>>entry2:nsrObjList.entrySet()){
                SootMethod nsrMethod=entry2.getKey();
                Map<String,Integer> objPosMap=entry2.getValue();
                int maxPos=-1;
                for(Map.Entry<String,Integer>entry3:objPosMap.entrySet()){
                    String obj=entry3.getKey();
                    int pos=entry3.getValue();
                    if((objList.contains(obj)||(obj.equals("all")&&type==2))&&maxPos<pos){
                        maxPos=pos;
                    }
                }
                if(maxPos!=-1){
                    tmp.put(nsrMethod,maxPos);
                }
            }
            if(tmp.size()!=0){
                res.put(toMethod, tmp);
            }
        }
        return res;
    }

    public Map<SootMethod,Map<String,Integer>> getNSRObj(List<SootMethod> ioTimeOutMethodList){
        Map<SootMethod,Map<String,Integer>> res=new LinkedHashMap<>();
        Iterator<SootMethod> ioTOIter=ioTimeOutMethodList.iterator();
        while(ioTOIter.hasNext()){
            // boolean isAll=false;
            SootMethod method=ioTOIter.next();
            boolean isInvoked=false;
            Map<String,Integer> info=new LinkedHashMap<>();
            // Map<Value,String> LR=new LinkedHashMap<>();
            Map<String,String> LR=new LinkedHashMap<>();
            boolean isDebug=false;
            if(method.getName().equals("run")){
                isDebug=false;
            }
            if(method.isConcrete()){
                JimpleBody body=(JimpleBody)method.retrieveActiveBody();
                BriefUnitGraph bug=new BriefUnitGraph(body);
                for(Unit u:bug){
                    Stmt stmt=(Stmt)u;
                    if(stmt.containsInvokeExpr()){
                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
                        if(invokeExpr.getMethod().getSubSignature().equals("void notify()")){
                            // System.out.println("ParseTimeOut | notify lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                            isInvoked=true;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void notifyAll()")){
                            // System.out.println("ParseTimeOut | notifyAll lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                            // System.out.println("ParseTimeOut | notifyAll stmt:"+stmt);
                            // isAll=true;
                            isInvoked=true;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void signal()")){
                            // System.out.println("ParseTimeOut | signal lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                            isInvoked=true;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void signalAll()")){
                            // System.out.println("ParseTimeOut | signalAll lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                            isInvoked=true;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void release()")){
                            // System.out.println("ParseTimeOut | release lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                            // System.out.println("ParseTimeOut | release stmt:"+stmt);
                            isInvoked=true;
                        }
                    }
                    if(isInvoked){
                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
                        int index=invokeExpr.getUseBoxes().size();
                        Value invokedValue=invokeExpr.getUseBoxes().get(0).getValue();
                        // System.out.println("ParseTimeOut | invokedValue:"+invokedValue);
                        if(invokedValue.toString().equals("this")){
                            info.put(method.getDeclaringClass().getName(), stmt.getJavaSourceStartLineNumber());
                            isInvoked=false;
                            continue;
                        }
                        
                        boolean isParameter=true;
                        for(Map.Entry<String,String>entry:LR.entrySet()){
                            String lV=entry.getKey();
                            String rV=entry.getValue();
                            // if(isAll){
                            //     info.put("all", stmt.getJavaSourceStartLineNumber());
                            //     isParameter=false;
                            //     break;
                            // }else{
                                if(lV.equals(invokedValue.toString())){
                                    info.put(rV, stmt.getJavaSourceStartLineNumber());
                                    if(isDebug){
                                        System.out.println(stmt.getJavaSourceStartLineNumber()+" found :"+stmt);
                                    }
                                    isParameter=false;
                                    break;
                                }
                        }
                        if(isParameter){
                            info.put(invokedValue.getType().toString(), stmt.getJavaSourceStartLineNumber());
                            if(isDebug){
                                System.out.println(stmt.getJavaSourceStartLineNumber()+" parameter :"+stmt);
                            }
                        }
                        isParameter=true;
                        isInvoked=false;
                    }else{
                        if(stmt instanceof AssignStmt){
                            AssignStmt assignStmt=(AssignStmt)stmt;
                            Value lV=assignStmt.getLeftOp();
                            Value rV=assignStmt.getRightOp();
                            if(stmt.containsFieldRef()){
                                FieldRef ref=stmt.getFieldRef();
                                SootField field=ref.getField();
                                LR.put(lV.toString(), field.getSignature());
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+":"+stmt);
                                    System.out.println("lV:"+lV.toString()+" field:"+field.getSignature());
                                }
                            }else{
                                LR.put(lV.toString(), rV.getType().toString());
                                if(isDebug){
                                    System.out.println(stmt.getJavaSourceStartLineNumber()+":"+stmt);
                                    System.out.println("lV:"+lV.toString()+" rV:"+rV.getType().toString());
                                }
                            }
                        }
                    }
                    // isAll=false;
                }
                if(info.size()!=0){
                    // System.out.println("ParseTimeOut | nsrMethod:"+method+" obj:"+info);
                    res.put(method, info);
                }
            }
        }
        return res;
    }




    /*
     * getWAObj
     */
    public List<String> getWATJSObj(SootMethod method,int option){
        List<String> res=new ArrayList<>();
        boolean isInvoked=false;
        // Map<Value,String> LR=new LinkedHashMap<>();
        Map<String,String> LR=new LinkedHashMap<>();
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    if(option==1&&invokeExpr.getMethod().getSubSignature().equals("void wait(int,long)")){
                        // System.out.println("ParseTimeOut | wait(int,long) lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                        // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                        isInvoked=true;
                    }
                    if(option==1&&invokeExpr.getMethod().getSubSignature().equals("void wait(long)")){
                        // System.out.println("ParseTimeOut | wait(long) lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                        // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                        isInvoked=true;
                    }
                    if(option==1&&invokeExpr.getMethod().getSubSignature().equals("boolean await(long,java.util.concurrent.TimeUnit)")){
                        // System.out.println("ParseTimeOut | preStmt:"+preStmt);
                        // System.out.println("ParseTimeOut | stmt:"+stmt);
                        // System.out.println("ParseTimeOut | await lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                        // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                        isInvoked=true; 
                    }
                    if(option==1&&invokeExpr.getMethod().getSubSignature().equals("long awaitNanos(long)")){
                        // System.out.println("ParseTimeOut | awaitNanos lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                        // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                        isInvoked=true; 
                    }
                    if(option==1&&invokeExpr.getMethod().getSubSignature().equals("boolean tryAcquire(long,java.util.concurrent.TimeUnit)")){
                        // System.out.println("ParseTimeOut | tryAcquire lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                        // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                        isInvoked=true; 
                    }
                    if(option==2&&invokeExpr.getMethod().getSubSignature().equals("void join(long,int)")){
                        // System.out.println("ParseTimeOut | join(long,int) lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                        // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                        isInvoked=true; 
                    }
                    if(option==2&&invokeExpr.getMethod().getSubSignature().equals("void join(long)")){
                        // System.out.println("ParseTimeOut | preStmt:"+preStmt);
                        // System.out.println("ParseTimeOut | stmt:"+stmt);
                        // int index=invokeExpr.getUseBoxes().size();
                        // Value invokedValue=invokeExpr.getUseBoxes().get(index-1).getValue();
                        // System.out.println("ParseTimeOut | invoke use value:"+invokeExpr.getUseBoxes());
                        // System.out.println("ParseTimeOut | invokedValue:"+invokedValue);
                        // System.out.println("ParseTimeOut | join(long) lineNum:"+stmt.getJavaSourceStartLineNumber()+" method:"+method);
                        // System.out.println(stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                        isInvoked=true; 
                    }
                    if(option==3&&invokeExpr.getMethod().getSubSignature().equals("void start()")){
                        isInvoked=true; 
                    }
                    if(option==4&&invokeExpr.getMethod().getSubSignature().equals("void run()")){
                        isInvoked=true; 
                    }
                }
                if(isInvoked){
                    // System.out.println("method:"+method+" :"+stmt.getJavaSourceStartLineNumber()+":"+stmt);
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    // int index=invokeExpr.getUseBoxes().size();
                    Value invokedValue=invokeExpr.getUseBoxes().get(0).getValue();
                    // System.out.println("invokeValue:"+invokedValue);
                    if(invokedValue.toString().equals("this")){
                        res.add(method.getDeclaringClass().getName());
                        isInvoked=false;
                        continue;
                    }
                    // System.out.println("ParseTimeOut | invokedValue:"+invokedValue);
                    boolean isParameter=true;
                    for(Map.Entry<String,String>entry:LR.entrySet()){
                        String lV=entry.getKey();
                        String rV=entry.getValue();
                        // System.out.println("ParseTimeOut | lV:"+lV+" rV:"+rV);
                        if(lV.equals(invokedValue.toString())){
                            if(!res.contains(rV)){
                                res.add(rV);
                            }
                            isParameter=false;
                            break;
                        }
                    }
                    if(isParameter){
                        res.add(invokedValue.getType().toString());
                    }
                    isParameter=true;
                    isInvoked=false;
                }else{
                    if(stmt instanceof AssignStmt){
                        AssignStmt assignStmt=(AssignStmt)stmt;
                        Value lV=assignStmt.getLeftOp();
                        Value rV=assignStmt.getRightOp();
                        if(stmt.containsFieldRef()){
                            FieldRef ref=stmt.getFieldRef();
                            SootField field=ref.getField();
                            LR.put(lV.toString(), field.getSignature());
                        }else{
                            LR.put(lV.toString(), rV.getType().toString());
                        }
                    }
                }
            }
        }
        if(res.size()!=0){
            // System.out.println("ParseTimeOut | watjMethod:"+method+" obj:"+res);
        }
        return res;
    }

    public boolean isContainNSR(SootMethod method){
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    if(invokeExpr.getMethod().getSubSignature().equals("void notify()")){
                        return true;
                    }
                    if(invokeExpr.getMethod().getSubSignature().equals("void notifyAll()")){
                        return true;
                    }
                    if(invokeExpr.getMethod().getSubSignature().equals("void signal()")){
                        return true;
                    }
                    if(invokeExpr.getMethod().getSubSignature().equals("void signalAll()")){
                        return true;
                    }
                    if(invokeExpr.getMethod().getSubSignature().equals("void release()")){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean isContainStart(SootMethod method){
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    if(invokeExpr.getMethod().getSubSignature().equals("void start()")){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public int getType(SootMethod method){
        int type=-1;
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    if(invokeExpr.getMethod().getSubSignature().equals("void wait(long)")||
                    invokeExpr.getMethod().getSubSignature().equals("void wait(long,int)")){
                        type=2;
                    }
                }
            }
        }
        return type;
    }
}
class ValueInfo{
    Value v=null;
    String value=null;
    String timeoutValue=null;
    int timeoutPos=-1;
    String lTimeOutValue=null;
    int lTimeOutPos=-1;
    String rTimeOutValue=null;
    int rTimeOutPos=-1;
    int type=-1;
    Value toValue=null;
    int pos=0;
    // 1:left 2:right
    int lr=0;
}
class InjectInfo{
    // pos
    int pos=0;
    // 1:left 2:right
    int lr=0;
    // 1:add 2:cmp
    int traceType=0;
    // comparison pos cmp|if
    int cmpPos=0;
}