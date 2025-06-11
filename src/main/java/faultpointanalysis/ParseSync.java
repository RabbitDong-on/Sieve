package faultpointanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import jsonutils.DelayPoint;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;

public class ParseSync{
    public String packageName;
    public String packageName_plus="dgdg";
    public List<SootMethod> inLockMethods;
    public ParseSync(String packageName,String packageName_plus,List<SootMethod> inLockMethods){
        this.packageName=packageName;
        this.packageName_plus=packageName_plus;
        this.inLockMethods=inLockMethods;
    }
    public static void main (String[] args){
        ParseSync parseSync=new ParseSync("1","2",new ArrayList<>());
        List<SootMethod>analyzedIO=new ArrayList<>();
        Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap=new LinkedHashMap<>();
        List<SootMethod>syncIOList=new ArrayList<>();
        syncIOList=parseSync.parseSyncIO(analyzedIO,csMap);
    }

    public List<SootMethod> parseSSIO(Set<DelayPoint> dpSet){
        List<SootMethod> res=new ArrayList<>();
        Iterator<DelayPoint> dpIterator=dpSet.iterator();
        while(dpIterator.hasNext()){
            DelayPoint dp=dpIterator.next();
            SootClass tgtSootClass=Scene.v().forceResolve(dp.className, SootClass.BODIES);
            SootMethod tgtMethod=tgtSootClass.getMethod(dp.funcName);
            int lineNum=dp.lineNum;
            if(isWithinSync(lineNum,tgtMethod)){
                res.add(tgtMethod);
            }
            if(tgtMethod.getName().equals("createLogDirectory")){
                res.add(tgtMethod);
            }
        }
        return res;
    }


    public List<SootMethod> parseSyncIO(List<SootMethod>analyzedIO,Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap){
        List<SootMethod> syncIOList=new ArrayList<>();
        List<SootMethod> tgtList=new ArrayList<>();
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            Map<SootMethod,Integer> tgtMap=entry.getKey();
            Map<SootMethod,Integer> srcMap=entry.getValue();
            for(Map.Entry<SootMethod,Integer>tgtEntry:tgtMap.entrySet()){
                SootMethod tgtMethod=tgtEntry.getKey();
                if(srcMap.size()==0){
                    // System.out.println("ishere!");
                    if(isWithinInLock(tgtMethod)){
                        tgtList.add(tgtMethod);
                        continue;
                    }
                }
                for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                    SootMethod srcMethod=srcEntry.getKey();
                    int tgtMethodPosition=srcEntry.getValue();     
                    if(isWithinSync(tgtMethodPosition,srcMethod)||isWithinInLock(tgtMethod)||isSyncMethodOrWithinSyncClass(tgtMethod)){
                        tgtList.add(tgtMethod);
                    }
                }
            }
        }
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
                syncIOList.add(analyzedIOMethod);
            }
        }
        return syncIOList;
    }
    
    public boolean isSystemClass(SootClass sootClass){
        // TODOEXTEND
        if(sootClass.getPackageName().contains(packageName)||sootClass.getPackageName().contains(packageName_plus)){
            return true;
        }
        return false;
    }
    
    public List<SootMethod> parseJoin(List<SootMethod>analyzedIO,Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap){
        List<SootMethod> res=new ArrayList<>();
        List<SootMethod> runMethodList=new ArrayList<>();
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            for(Map.Entry<SootMethod,Integer>en:entry.getKey().entrySet()){
                SootMethod method=en.getKey();
                if(method.getSubSignature().equals("void run()")&&!runMethodList.contains(method)){
                    runMethodList.add(method);
                }
            }
        }
        res=color(csMap, runMethodList, analyzedIO);
        return res;
    }

    public List<SootMethod> parserJoin(List<SootMethod>analyzedIO,Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,CallGraph cg){
        List<SootMethod> res=new ArrayList<>();
        List<SootMethod> startMethodList=new ArrayList<>();
        Map<List<String>,SootMethod> startObjList2M=new LinkedHashMap<>();
        Map<List<SootMethod>,List<String>> run2ObjList=new LinkedHashMap<>();
        List<SootMethod> tgtList=new ArrayList<>();
        List<String> joinObjList=new ArrayList<>();
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
                List<String> sObj=getWAJSObj(sootMethod,3);
                if(sObj.size()!=0){
                    startObjList2M.put(sObj, sootMethod);
                }
                List<String> jObj=getWAJSObj(sootMethod,2);
                if(jObj.size()!=0){
                    joinObjList.addAll(jObj);
                }
                List<String> rObj=getWAJSObj(sootMethod, 4);
                if(rObj.size()!=0){
                    List<SootMethod> runMethod=getRunMethod(sootMethod);
                    run2ObjList.put(runMethod, rObj);
                }
            }
        }
        // get sync start
        for(Map.Entry<List<String>,SootMethod>entry:startObjList2M.entrySet()){
            List<String> sObjList=entry.getKey();
            SootMethod startMethod=entry.getValue();
            Iterator<String> sObjIter=sObjList.iterator();
            while(sObjIter.hasNext()){
                String sObj=sObjIter.next();
                if(joinObjList.contains(sObj)&&!startMethodList.contains(startMethod)){
                    startMethodList.add(startMethod);
                    break;
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
        // join->start->run
        Iterator<SootMethod> sIter=startMethodList.iterator();
        while(sIter.hasNext()){
            SootMethod syncStartMethod=sIter.next();
            Iterator<MethodOrMethodContext> callees=new Targets(cg.edgesOutOf(syncStartMethod));
            while(callees.hasNext()){
                SootMethod callee=(SootMethod)callees.next();
                if(callee.getSubSignature().equals("void run()")){
                    if(runMethodList.contains(callee)&&!tgtList.contains(callee)){
                        tgtList.add(callee);
                    }
                }
            }
        }
        // join->run
        for(Map.Entry<List<SootMethod>,List<String>>entry:run2ObjList.entrySet()){
            List<SootMethod> runMList=entry.getKey();
            List<String> runObj=entry.getValue();
            Iterator<String> runIter=runObj.iterator();
            while(runIter.hasNext()){
                String obj=runIter.next();
                if(joinObjList.contains(obj)){
                    Iterator<SootMethod> runMIter=runMList.iterator();
                    while(runMIter.hasNext()){
                        SootMethod runM=runMIter.next();
                        if(runMethodList.contains(runM)&&!tgtList.contains(runM)){
                            tgtList.add(runM);
                        }
                    }
                }
            }
        }
        res=color(csMap, tgtList, analyzedIO);
        return res;
    }
    public List<SootMethod> getRunMethod(SootMethod method){
        List<SootMethod> res=new ArrayList<>();
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    if(invokeExpr.getMethod().getSubSignature().equals("void run()")){
                        res.add(invokeExpr.getMethod());
                    }
                }
            }
        }
        return res;
    }

    public List<SootMethod> parseNSSyncIO(List<SootMethod>analyzedIO,Map<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csMap,Set<DelayPoint>dpSet){
        List<SootMethod> res=new ArrayList<>();
        List<SootMethod> nsMethodList=new ArrayList<>();
        // List<String> waObjList=new ArrayList<>();
        for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>entry:csMap.entrySet()){
            Map<SootMethod,Integer> tgtMap=entry.getKey();
            for(Map.Entry<SootMethod,Integer>tgtEntry:tgtMap.entrySet()){
                SootMethod tgtMethod=tgtEntry.getKey();
                if(isContainNS(tgtMethod)){
                    nsMethodList.add(tgtMethod);
                }
            }
        }

        // Map<SootMethod,Map<String,Integer>> nsObj=new LinkedHashMap<>();
        // nsObj=getNSObj(nsMethodList);
        Map<SootMethod,Integer> syncNSMethod=new LinkedHashMap<>();

        syncNSMethod=getSyncMethod(nsMethodList);
        List<SootMethod> tgtList=new ArrayList<>();
        for(Map.Entry<SootMethod,Integer>entry:syncNSMethod.entrySet()){
            SootMethod nsMethod=entry.getKey();
            int pos=entry.getValue();
            for(Map.Entry<Map<SootMethod,Integer>,Map<SootMethod,Integer>>csEntry:csMap.entrySet()){
                Map<SootMethod,Integer> srcMap=csEntry.getValue();
                for(Map.Entry<SootMethod,Integer>srcEntry:srcMap.entrySet()){
                    SootMethod srcMethod=srcEntry.getKey();
                    int tgtPos=srcEntry.getValue();
                    if(srcMethod.equals(nsMethod)&&tgtPos<pos){
                        tgtList.add(srcMethod);
                    }
                }
            }
            Iterator<DelayPoint> dpIter=dpSet.iterator();
            while(dpIter.hasNext()){
                DelayPoint dp=dpIter.next();
                if(nsMethod.getDeclaringClass().getName().equals(dp.className)&&
                nsMethod.getSubSignature().equals(dp.funcName)&&
                dp.lineNum<pos){
                    tgtList.add(nsMethod);
                }
            }
        }
        res=color(csMap, tgtList, analyzedIO);
        return res;
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
    
    public Map<SootMethod,Integer> getSyncNSMethod(Map<SootMethod,Map<String,Integer>>nsObj,List<String>waObjList){
        Map<SootMethod,Integer> res=new LinkedHashMap<>();
        for(Map.Entry<SootMethod,Map<String,Integer>>entry:nsObj.entrySet()){
            SootMethod nsMethod=entry.getKey();
            Map<String,Integer> nsObj2Pos=entry.getValue();
            int maxPos=-1;
            for(Map.Entry<String,Integer>entry2:nsObj2Pos.entrySet()){
                String obj=entry2.getKey();
                int pos=entry2.getValue();
                if((waObjList.contains(obj)||obj.equals("all"))&&maxPos<pos){
                    maxPos=pos;
                }
            }
            if(maxPos!=-1){
                res.put(nsMethod, maxPos);
            }
        }
        return res;
    }

    public Map<SootMethod,Integer> getSyncMethod(List<SootMethod>nsMethodlist){
        Map<SootMethod,Integer> res=new LinkedHashMap<>();
        Iterator<SootMethod> nsIter=nsMethodlist.iterator();
        while(nsIter.hasNext()){
            SootMethod method=nsIter.next();
            if(method.isConcrete()){
                JimpleBody body=(JimpleBody)method.retrieveActiveBody();
                BriefUnitGraph bug=new BriefUnitGraph(body);
                for(Unit u:bug){
                    Stmt stmt=(Stmt)u;
                    if(stmt.containsInvokeExpr()){
                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
                        if(invokeExpr.getMethod().getSubSignature().equals("void notify()")){
                            res.put(method, stmt.getJavaSourceStartLineNumber());
                            break;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void notifyAll()")){
                            res.put(method, stmt.getJavaSourceStartLineNumber());
                            break;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void signal()")){
                            res.put(method, stmt.getJavaSourceStartLineNumber());
                            break;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void signalAll()")){
                            res.put(method, stmt.getJavaSourceStartLineNumber());
                            break;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void countDown()")){
                            res.put(method, stmt.getJavaSourceStartLineNumber());
                            break;
                        }
                    }
                }
            }

        }
        return res;
    }

    public List<String> getWAJSObj(SootMethod method,int option){
        List<String> res=new ArrayList<>();
        boolean isInvoked=false;
        Map<String,String> LR=new LinkedHashMap<>();
        if(method.isConcrete()){
            JimpleBody body=(JimpleBody)method.retrieveActiveBody();
            BriefUnitGraph bug=new BriefUnitGraph(body);
            for(Unit u:bug){
                Stmt stmt=(Stmt)u;
                if(stmt.containsInvokeExpr()){
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    if(option==1&&invokeExpr.getMethod().getSubSignature().equals("void wait()")){
                        isInvoked=true;
                    }
                    if(option==1&&invokeExpr.getMethod().getSubSignature().equals("void await()")){
                        isInvoked=true; 
                    }
                    if(option==2&&invokeExpr.getMethod().getSubSignature().equals("void join()")){
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
                    InvokeExpr invokeExpr=stmt.getInvokeExpr();
                    int index=invokeExpr.getUseBoxes().size();
                    Value invokedValue=invokeExpr.getUseBoxes().get(index-1).getValue();
                    if(invokedValue.toString().equals("this")){
                        res.add(method.getDeclaringClass().getName());
                        isInvoked=false;
                        continue;
                    }
                    boolean isParameter=true;
                    for(Map.Entry<String,String>entry:LR.entrySet()){
                        String lV=entry.getKey();
                        String rV=entry.getValue();
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
        return res;
    }
    /*
     * getNSObj
     */
    public Map<SootMethod,Map<String,Integer>> getNSObj(List<SootMethod> nsMethodList){
        Map<SootMethod,Map<String,Integer>>res=new LinkedHashMap<>();
        Iterator<SootMethod> nsIter=nsMethodList.iterator();
        while(nsIter.hasNext()){
            boolean isAll=false;
            boolean isInvoked=false;
            Map<String,Integer> info=new LinkedHashMap<>();
            Map<String,String> LR=new LinkedHashMap<>();
            SootMethod nsMethod=nsIter.next();
            if(nsMethod.isConcrete()){
                JimpleBody body=(JimpleBody)nsMethod.retrieveActiveBody();
                BriefUnitGraph bug=new BriefUnitGraph(body);
                for(Unit u:bug){
                    Stmt stmt=(Stmt)u;
                    if(stmt.containsInvokeExpr()){
                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
                        if(invokeExpr.getMethod().getSubSignature().equals("void notify()")){
                            isInvoked=true;
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void notifyAll()")){
                            isAll=true;
                            isInvoked=true; 
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void signal()")){
                            isInvoked=true; 
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void signalAll()")){
                            isInvoked=true; 
                        }
                        if(invokeExpr.getMethod().getSubSignature().equals("void countDown()")){
                            isInvoked=true; 
                        }
                    }
                    if(isInvoked){
                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
                        int index=invokeExpr.getUseBoxes().size();
                        Value invokedValue=invokeExpr.getUseBoxes().get(index-1).getValue();
                        if(invokedValue.toString().equals("this")){
                            info.put(nsMethod.getDeclaringClass().getName(), stmt.getJavaSourceStartLineNumber());
                            isInvoked=false;
                            continue;
                        }
                        boolean isParameter=true;
                        for(Map.Entry<String,String>entry:LR.entrySet()){
                            String lV=entry.getKey();
                            String rV=entry.getValue();
                            if(isAll){
                                info.put("all", stmt.getJavaSourceStartLineNumber());
                                isParameter=false;
                                break;
                            }else{
                                if(lV.equals(invokedValue.toString())){
                                    info.put(rV, stmt.getJavaSourceStartLineNumber());
                                    isParameter=false;
                                    break;
                                }
                            }
                        }
                        if(isParameter){
                            info.put(invokedValue.getType().toString(), stmt.getJavaSourceStartLineNumber());
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
                    isAll=false;
                }
                if(info.size()!=0){
                    res.put(nsMethod, info);
                }
            }
        }
        return res;
    }

    public boolean isContainNS(SootMethod method){
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
                    if(invokeExpr.getMethod().getSubSignature().equals("void countDown()")){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isSyncMethodOrWithinSyncClass(SootMethod method){
        if(method.isSynchronized()||method.getDeclaringClass().isSynchronized()){
            return true;
        }
        return false;
    }

    public boolean isWithinSync(int tgtMethodPosition,SootMethod srcMethod){
        JimpleBody body=(JimpleBody)srcMethod.retrieveActiveBody();
        boolean withinSync=false;
        int syncLine=-1;
        boolean withinLock=false;
        boolean syncStart=false;
        boolean lockStart=false;
        boolean isInvoked=true;
        // if(srcMethod.getName().equals("getOrCreateLog")){
        //     System.out.println("ParseSync | tgtMethodPos:"+tgtMethodPosition);
        // }
        for(Unit u:body.getUnits()){
            Stmt stmt=(Stmt)u;
            // if(srcMethod.getName().equals("getOrCreateLog")){
            //     System.out.println("pos:"+stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
            // }
            List<ValueBox> useBox=stmt.getUseBoxes();
            if(stmt.getJavaSourceStartLineNumber()==tgtMethodPosition){
                isInvoked=true;
            }
            if((!withinSync&&!withinLock)&&(stmt.getJavaSourceStartLineNumber()>tgtMethodPosition)){
                return false;
            }
            if(useBox.size()==1){
                ValueBox vb=useBox.get(0);
                if(stmt.toString().equals("entermonitor "+vb.getValue().toString())){
                    // if(srcMethod.getName().equals("getOrCreateLog")){
                    //     System.out.println("pos:"+stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                    // }
                    withinSync=true;
                    syncLine=stmt.getJavaSourceStartLineNumber();
                }
                if(stmt.toString().equals("exitmonitor "+vb.getValue().toString())&&(stmt.getJavaSourceStartLineNumber()==syncLine)){
                    // if(srcMethod.getName().equals("getOrCreateLog")){
                    //     System.out.println("pos:"+stmt.getJavaSourceStartLineNumber()+" stmt:"+stmt);
                    // }
                    withinSync=false;
                    syncLine=-1;
                }
            }
            if(withinSync){
                int currentPos=stmt.getJavaSourceStartLineNumber();
                if(currentPos==-1){
                    continue;
                }
                if(tgtMethodPosition>=currentPos){
                    syncStart=true;
                }
                if((tgtMethodPosition<=currentPos)&&syncStart&&isInvoked){
                    return true;
                }
            }else{
                syncStart=false;
            }
            if(stmt.containsInvokeExpr()){
                InvokeExpr invokeExpr=stmt.getInvokeExpr();
                SootMethod sootMethod=invokeExpr.getMethod();
                if(sootMethod.getDeclaringClass().getName().equals("java.util.concurrent.locks.ReentrantLock")||
                   sootMethod.getDeclaringClass().getName().equals("java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock")||
                   sootMethod.getDeclaringClass().getName().equals("java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock")){
                    if(sootMethod.getName().equals("lock")){
                        withinLock=true;
                    }
                    if(sootMethod.getName().equals("unlock")){
                        withinLock=false;
                    }
                }
            }

            if(withinLock){
                int currentPos=stmt.getJavaSourceStartLineNumber();
                if(currentPos==-1){
                    continue;
                }
                if(tgtMethodPosition>=currentPos){
                    lockStart=true;
                }
                if((tgtMethodPosition<=currentPos)&&lockStart&&isInvoked){
                    return true;
                }
            }else{
                lockStart=false;
            }
        }
        return false;
    }
    public boolean isWithinInLock(SootMethod tgtMethod){
        Iterator<SootMethod> inLockMethodIter=inLockMethods.iterator();
        while(inLockMethodIter.hasNext()){
            SootMethod inLockMethod=inLockMethodIter.next();
            if(inLockMethod.isConcrete()){
                JimpleBody body=(JimpleBody)inLockMethod.retrieveActiveBody();
                for(Unit u:body.getUnits()){
                    Stmt stmt=(Stmt)u;
                    if(stmt.containsInvokeExpr()){
                        SootMethod bMethod=stmt.getInvokeExpr().getMethod();
                        if(bMethod.isConcrete()&&bMethod.getName().contains("bootstrap")){
                            SootClass sClass=bMethod.getDeclaringClass();
                            List<SootMethod>lsm=sClass.getMethods();
                            Iterator<SootMethod> lsmIter=lsm.iterator();
                            while(lsmIter.hasNext()){
                                SootMethod sm=lsmIter.next();
                                if(sm.getName().contains("apply")){
                                    JimpleBody body2=(JimpleBody)sm.retrieveActiveBody();
                                    for(Unit uu:body2.getUnits()){
                                        Stmt stmt2=(Stmt)uu;
                                        if(stmt2.containsInvokeExpr()){
                                            SootMethod aMethod=stmt2.getInvokeExpr().getMethod();
                                            if(aMethod.isConcrete()){
                                                JimpleBody body3=(JimpleBody)aMethod.retrieveActiveBody();
                                                for(Unit uuu:body3.getUnits()){
                                                    Stmt stmt3=(Stmt)uuu;
                                                    if(stmt3.containsInvokeExpr()){
                                                        if(stmt3.getInvokeExpr().getMethod().getSignature().equals(tgtMethod.getSignature())){
                                                            return true;
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
            }
        }
        return false;
    }
}