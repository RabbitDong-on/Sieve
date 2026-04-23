package faultinjection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import com.alibaba.fastjson2.JSON;

import client.FailSlowAgent;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import jsonutils.ConflictPair;
import jsonutils.DP2ConflictPair;
import jsonutils.DP2ConflictPairs;
import jsonutils.JsonUtil;
import jsonutils.MemAccess;

public class ScheduleMemAccesses {
    public String fileName;
    public String sysClassPath;
    List<DP2ConflictPair> dp2ConflictedPairs=new ArrayList<>();

    public ScheduleMemAccesses(String fileName,String sysClassPath){
        this.fileName=fileName;
        this.sysClassPath=sysClassPath;
    }

    public static void main(String[] args){

    }
    // zooDP2ConflictPairs
    public void schedule()throws Exception{
        List<Integer> toMAList=new ArrayList<>();
        List<Integer> noMAList=new ArrayList<>();
        // javassist config
        ClassPool pool=ClassPool.getDefault();
        pool.insertClassPath(sysClassPath);
        // read dp2conflictpairs
        JsonUtil jsonUtil=new JsonUtil();
        String path=System.getProperty("user.dir");
        String context=jsonUtil.readJson(path,fileName);
        DP2ConflictPairs dp2ConflictPairs=JSON.parseObject(context,DP2ConflictPairs.class);
        Iterator<DP2ConflictPair> dp2CIter=dp2ConflictPairs.dp2ConflictedPairs.iterator();
        while(dp2CIter.hasNext()){
            DP2ConflictPair dPair=dp2CIter.next();
            String dpClzName=dPair.className;
            String dpFuncName=dPair.funcName;
            int dpLineNum=dPair.lineNum;
            List<ConflictPair> conflictPairs=dPair.conflictPairs;
            Iterator<ConflictPair> cPIter=conflictPairs.iterator();
            while(cPIter.hasNext()){
                ConflictPair conflictPair=cPIter.next();
                MemAccess toMA=conflictPair.toMemAccess;
                int toId=conflictPair.toId;
                MemAccess noMA=conflictPair.normalMemAccess;
                int noId=conflictPair.noId;
                // need to deduplication
                if(!toMAList.contains(toId)){
                    scheduleMA(noMA,noId,pool,2);
                    toMAList.add(toId);
                }
                if(!noMAList.contains(noId)){
                    scheduleMA(toMA,toId,pool,1);
                    noMAList.add(noId);
                }
            }
        }
        System.out.println("ScheduleMemAccesses | toMAList:"+toMAList.size());
        System.out.println("ScheduleMemAccesses | noMALIst:"+noMAList.size());
    }

    /*
     * scheduleMA
     * 1. inject await before normal memory accesses
     * 2. inject signal after timeout handler memory accesses
     */
    public void scheduleMA(MemAccess mA, int id,ClassPool pool,int type){
        try{
            String toClzName=mA.className;
            String toFuncName=mA.funcName;
            int toLineNum=mA.lineNum;
            CtClass ctClass=pool.get(toClzName);
            if(ctClass.isFrozen()){
                ctClass.defrost();
            }
            ClassFile classFile=ctClass.getClassFile();
            ConstPool constPool=classFile.getConstPool();
            for(CtBehavior ctBehavior:ctClass.getDeclaredBehaviors()){
                if(ctBehavior.isEmpty()){
                    continue;
                }
                MethodInfo methodInfo=ctBehavior.getMethodInfo();
                if(!toFuncName.equals(methodInfo.getName())){
                    continue;
                }
                CodeAttribute codeAttribute=methodInfo.getCodeAttribute();
                for(CodeIterator codeIter=codeAttribute.iterator();codeIter.hasNext();){
                    int address=codeIter.next();
                    int opcode=codeIter.byteAt(address);
                    int pos=methodInfo.getLineNumber(address);
                    if(toLineNum==pos){
                        switch(opcode){
                            case Opcode.GETSTATIC:
                            case Opcode.GETFIELD:
                            case Opcode.PUTSTATIC:
                            case Opcode.PUTFIELD:
                                // inject synchronized method
                                Bytecode bytecode=new Bytecode(constPool);
                                if(type==1){
                                    // timeoout handler memory accesses
                                    // inject signal after them
                                    bytecode.addLdc2w((long)id);
                                    bytecode.addInvokestatic(FailSlowAgent.class.getName(), "signal", "(J)V");
                                    codeIter.insert(bytecode.get());
                                }else{
                                    // normal execution memory accesses
                                    // inject await before them
                                    bytecode.addLdc2w((long)id);
                                    bytecode.addInvokestatic(FailSlowAgent.class.getName(), "await", "(J)V");
                                    codeIter.insert(address, bytecode.get());
                                }
                        }
                    }
                }
                codeAttribute.computeMaxStack();
            }
            ctClass.writeFile(sysClassPath);
        }catch(Exception e){
            // ignore
            e.printStackTrace();
        }
    }
}
