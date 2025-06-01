package faultpointanalysis;


import java.util.Iterator;

import com.alibaba.fastjson2.JSON;

import client.FailSlowAgent;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;
import javassist.bytecode.Opcode;
import jsonutils.DelayPoint;
import jsonutils.JsonUtil;
import jsonutils.SystemInfo;


public class NonBlockingIOProcess{

    public String fileName;
    public String sysName;
    public String sysClassPath;
    public NonBlockingIOProcess(String fileName,String sysName,String sysClassPath){
        this.fileName=fileName;
        this.sysName=sysName;
        this.sysClassPath=sysClassPath;
    }
    public static void main(String[] args){
    }

    public void InjectHook(int nodeId)throws Exception{
        JsonUtil jsonUtil=new JsonUtil();
        String path=System.getProperty("user.dir");
        String context=jsonUtil.readJson(path, fileName);
        SystemInfo systemInfo=JSON.parseObject(context, SystemInfo.class);
        ClassPool pool=ClassPool.getDefault();
        pool.insertClassPath(sysClassPath);
        Iterator<DelayPoint> iter=systemInfo.dpSet.iterator();
        int success=0;
        int fail=0;
        while(iter.hasNext()){
            DelayPoint dp=iter.next();
            String className=dp.className;
            String funcName=dp.funcName;
            int lineNum=dp.lineNum;
            if(className.equals("org.apache.zookeeper.server.ReferenceCountedACLCache")||
            className.equals("org.apache.zookeeper.server.persistence.FileSnap")){
                continue;
            }
            CtClass ctClass=pool.get(className);
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
                if(!funcName.contains(methodInfo.getName())){
                    continue;
                }
                CodeAttribute codeAttribute=methodInfo.getCodeAttribute();
                boolean isInject=false;
                int step=0;
                for(CodeIterator codeIter=codeAttribute.iterator();codeIter.hasNext();){
                    int address=codeIter.next();
                    int pos=methodInfo.getLineNumber(address);
                    int opcode=codeIter.byteAt(address);
                    if(pos==lineNum){
                        if(Mnemonic.OPCODE[opcode].contains("invoke")){
                            step++;
                        }
                    }
                    if(lineNum==pos&&!isInject){
                        Bytecode bytecode=new Bytecode(constPool);
                        // insert before the fault point
                        if(nodeId==1){
                            bytecode.add(Opcode.ICONST_1);
                        }else if(nodeId==2){
                            bytecode.add(Opcode.ICONST_2);
                        }else if(nodeId==3){
                            bytecode.add(Opcode.ICONST_3);
                        }else if(nodeId==4){
                            bytecode.add(Opcode.ICONST_4);
                        }
                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceNonBlockingIOStartTime", "(I)V");
                        codeIter.insert(address, bytecode.get());
                        isInject=true;
                    }
                }
                int count=0;
                for(CodeIterator codeIter=codeAttribute.iterator();codeIter.hasNext();){
                    int address=codeIter.next();
                    int pos=methodInfo.getLineNumber(address);
                    int opcode=codeIter.byteAt(address);
                    if(pos==lineNum){
                        if(Mnemonic.OPCODE[opcode].contains("invoke")){
                            count++;
                        }
                        if(count==step+1){
                            Bytecode bytecode=new Bytecode(constPool);
                            if(nodeId==1){
                                bytecode.add(Opcode.ICONST_1);
                            }else if(nodeId==2){
                                bytecode.add(Opcode.ICONST_2);
                            }else if(nodeId==3){
                                bytecode.add(Opcode.ICONST_3);
                            }else if(nodeId==4){
                                bytecode.add(Opcode.ICONST_4);
                            }
                            bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceNonBlockingIOEndTime", "(I)V");
                            codeIter.insertEx(bytecode.get());
                            break;
                        }
                    }
                }
                codeAttribute.computeMaxStack();
            }
            try{
                ctClass.writeFile(sysClassPath);
                success++;
            }catch(CannotCompileException cce){
                fail++;
                System.out.println("NonBlockingIOProcess.java | CannotCompileException class:"+className+"---func:"+funcName+"---line:"+lineNum);
            }
        }
        System.out.println("NonBlockingIOProcess.java | success:"+success+"    fail:"+fail);
    }
}