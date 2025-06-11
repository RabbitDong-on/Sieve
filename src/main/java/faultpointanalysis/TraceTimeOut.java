package faultpointanalysis;


import java.io.IOException;
import java.util.Iterator;
import com.alibaba.fastjson.JSON;
import client.FailSlowAgent;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;
import javassist.bytecode.Opcode;
import jsonutils.JsonUtil;
import jsonutils.SystemInfo;
import jsonutils.TimeOutPoint;


/*
 * TraceTimeOut
 */
public class TraceTimeOut{
    private static boolean isDebug=false;
    public static final String zooSourceDir="/failslow/zookeeper/zookeeper-server/target/classes";
    public String fileName;
    public String sysName;
    public String sysClassPath;
    public TraceTimeOut(String fileName,String sysName,String sysClassPath){
        this.fileName=fileName;
        this.sysClassPath=sysClassPath;
        this.sysName=sysName;
    }
    public static void main(String[] args){
        if(args.length!=1){
            System.out.println("TraceTimeOut.java | please specify system name");
            System.exit(0);
        }
        TraceTimeOut traceTimeOut=new TraceTimeOut("zooTOPoint","zoo", zooSourceDir);
        traceTimeOut.traceAllTimeOut();
    }
    public void traceAllTimeOut(){
        JsonUtil jsonUtil=new JsonUtil();
        String path=System.getProperty("user.dir");
        String context=jsonUtil.readJson(path, fileName);
        SystemInfo systemInfo=JSON.parseObject(context, SystemInfo.class);
        ClassPool pool=ClassPool.getDefault();
        try{
            // TODOEXTEND
            pool.insertClassPath(sysClassPath);
            Iterator<TimeOutPoint> toIter=systemInfo.toSet.iterator();
            while(toIter.hasNext()){
                TimeOutPoint toP=toIter.next();
                CtClass ctClass=pool.get(toP.toClassName);
                if(ctClass.isFrozen()){
                    ctClass.defrost();
                }
                ClassFile classFile=ctClass.getClassFile();
                ConstPool constPool=classFile.getConstPool();
                traceTimeOut(ctClass, constPool, toP);
            }
        }catch(NotFoundException nfe){
            nfe.printStackTrace();
        }
    }
    /*
     * traceTimeOut
     */
    public void traceTimeOut(CtClass ctClass, ConstPool constPool,TimeOutPoint toP){
        try{
            boolean isInserted=false;
            if(isDebug){
                System.out.println("TraceTimeOut.java | className:"+toP.toClassName);
                System.out.println("TraceTimeOut.java | funcName:"+toP.toFuncName);
                System.out.println("TraceTimeOut.java | position:"+toP.toPos);
                System.out.println("TraceTimeOut.java | timeoutMethodType:"+toP.timeoutMethodType);
            }
            for(CtBehavior ctBehavior:ctClass.getDeclaredBehaviors()){
                if(isInserted){
                    break;
                }
                // System.out.println("CtBehavior:"+ctBehavior);
                if(ctBehavior.isEmpty()){
                    continue;
                }
                MethodInfo methodInfo=ctBehavior.getMethodInfo();
                if(!toP.toFuncName.contains(methodInfo.getName())){
                    continue;
                }
                if(isDebug){
                    System.out.println("TraceTimeOut.java | MethodInfo:"+methodInfo.getName());
                }
                CodeAttribute codeAttribute=methodInfo.getCodeAttribute();
                if(isDebug){
                    printAllBytecode(codeAttribute);
                }
                for(CodeIterator codeIter=codeAttribute.iterator();codeIter.hasNext();){
                    int address=codeIter.next();
                    int opcode=codeIter.byteAt(address);
                    int pos=methodInfo.getLineNumber(address);
                    if(toP.toPos==pos){
                        // System.out.println("opcode:"+Mnemonic.OPCODE[opcode]);
                        if(toP.timeoutMethodType==3){
                            // System.out.println("TraceTimeOut.java | traceType:"+toP.traceType+" lr:"+toP.lr+" opcode:"+Mnemonic.OPCODE[opcode]);
                            // customized timeout value
                            if((opcode==Opcode.LADD||opcode==Opcode.LSUB)&&toP.traceType==1){
                                if(isDebug){
                                    System.out.println("TraceTimeOut.java | insert add"+" lr:"+toP.lr);
                                }
                                Bytecode bytecode=new Bytecode(constPool);
                                bytecode.add(Opcode.DUP2_X2);
                                if(toP.lr==1){
                                    bytecode.add(Opcode.ICONST_1);
                                }else{
                                    bytecode.add(Opcode.ICONST_2);
                                }
                                bytecode.addInvokestatic(FailSlowAgent.class.getName(),"traceAddSub","(JJI)J");
                                bytecode.add(Opcode.DUP2_X2);
                                bytecode.add(Opcode.POP2);
                                codeIter.insert(address, bytecode.get());
                           }else if((opcode==Opcode.LCMP)&&toP.traceType==2){
                                if(isDebug){
                                    System.out.println("TraceTimeOut.java | insert CMP"+" lr:"+toP.lr);
                                }
                                Bytecode bytecode=new Bytecode(constPool);
                                bytecode.add(Opcode.DUP2_X2);
                                if(toP.lr==1){
                                    bytecode.add(Opcode.ICONST_1);
                                }else{
                                    bytecode.add(Opcode.ICONST_2);
                                }
                                bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceCMP", "(JJI)J");
                                bytecode.add(Opcode.DUP2_X2);
                                bytecode.add(Opcode.POP2);
                                codeIter.insert(address, bytecode.get());
                           }else if((opcode==Opcode.IF_ICMPGE||opcode==Opcode.IF_ICMPGT||opcode==Opcode.IF_ICMPLE||opcode==Opcode.IF_ICMPLT)&&toP.traceType==2){
                                if(isDebug){
                                    System.out.println("TraceTimeOut.java | insert CMP"+" lr:"+toP.lr);
                                }
                                Bytecode bytecode=new Bytecode(constPool);
                                bytecode.add(Opcode.DUP2);
                                if(toP.lr==1){
                                    bytecode.add(Opcode.ICONST_1);
                                }else{
                                    bytecode.add(Opcode.ICONST_2);
                                }
                                bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceCMP", "(III)V");
                                codeIter.insert(address, bytecode.get());
                           }
                        }else{
                            if(opcode==Opcode.INVOKEVIRTUAL){
                                Bytecode bytecode=new Bytecode(constPool);
                                int operand=codeIter.s16bitAt(address+1);
                                String methodName=constPool.getMethodrefName(operand);
                                String methodType=constPool.getMethodrefType(operand);
                                
                                // System.out.println("methodName:"+methodName+" methodType:"+methodType);
                                if(methodName.equals("connect")&&methodType.equals("(Ljava/net/SocketAddress;I)V")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert connect");
                                    }
                                    isInserted=true;
                                    bytecode.add(Opcode.DUP);
                                    bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceConnect", "(I)V");
                                }else if(methodName.equals("wait")&&methodType.equals("(J)V")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert wait");
                                    }
                                    isInserted=true;
                                    bytecode.add(Opcode.DUP2);
                                    bytecode.addInvokestatic(FailSlowAgent.class.getName(),"traceWait","(J)V");
                                }else if(methodName.equals("wait")&&methodType.equals("(JI)V")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert wait");
                                    }
                                    isInserted=true;
                                    bytecode.add(Opcode.DUP_X2);
                                    bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceWait","(JI)J");
                                    bytecode.add(Opcode.DUP2_X1);
                                    bytecode.add(Opcode.POP2);
                                }else if(methodName.equals("tryAcquire")&&methodType.equals("(JLjava/util/concurrent/TimeUnit;)Z")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert tryacquire");
                                    }
                                    isInserted=true;
                                    int preoperand=codeIter.s16bitAt(address-2);
                                    String fieldName=constPool.getFieldrefName(preoperand);
                                    if(fieldName==null){
                                        bytecode.add(Opcode.DUP_X2);
                                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceTryAcquire", "(JLjava/util/concurrent/TimeUnit;)J");
                                        bytecode.add(Opcode.DUP2_X1);
                                        bytecode.add(Opcode.POP2);
                                    }else{
                                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceTryAcquire", "(JLjava/util/concurrent/TimeUnit;)J");
                                        bytecode.addGetstatic("java.util.concurrent.TimeUnit",fieldName,"Ljava/util/concurrent/TimeUnit;");
                                    }
                                }else if(methodName.equals("join")&&methodType.equals("(J)V")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert join");
                                    }
                                    isInserted=true;
                                    bytecode.add(Opcode.DUP2);
                                    bytecode.addInvokestatic(FailSlowAgent.class.getName(),"traceJoin","(J)V");
                                }else if(methodName.equals("join")&&methodType.equals("(JI)V")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert join");
                                    }
                                    isInserted=true;
                                    bytecode.add(Opcode.DUP_X2);
                                    bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceJoin","(JI)J");
                                    bytecode.add(Opcode.DUP2_X1);
                                    bytecode.add(Opcode.POP2);
                                }else if(methodName.equals("await")&&methodType.equals("(JLjava/util/concurrent/TimeUnit;)Z")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert await");
                                    }
                                    isInserted=true;
                                    int preoperand=codeIter.s16bitAt(address-2);
                                    String fieldName=constPool.getFieldrefName(preoperand);
                                    if(fieldName==null){
                                        bytecode.add(Opcode.DUP_X2);
                                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceAwait", "(JLjava/util/concurrent/TimeUnit;)J");
                                        bytecode.add(Opcode.DUP2_X1);
                                        bytecode.add(Opcode.POP2);
                                    }else{
                                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceAwait", "(JLjava/util/concurrent/TimeUnit;)J");
                                        bytecode.addGetstatic("java.util.concurrent.TimeUnit",fieldName,"Ljava/util/concurrent/TimeUnit;");
                                    }
                                }
                                codeIter.insert(address,bytecode.get());
                            }else if(opcode==Opcode.INVOKEINTERFACE){
                                Bytecode bytecode=new Bytecode(constPool);
                                int operand=codeIter.s16bitAt(address+1);
                                String methodName=constPool.getMethodrefName(operand);
                                String methodType=constPool.getMethodrefType(operand);
                                if(methodName.equals("awaitNanos")&&methodType.equals("(J)J")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert await");
                                    }
                                    isInserted=true;
                                    bytecode.add(Opcode.DUP2);
                                    bytecode.addInvokestatic(FailSlowAgent.class.getName(),"traceAwait","(J)V");
                                }else if(methodName.equals("await")&&methodType.equals("(JLjava/util/concurrent/TimeUnit;)Z")){
                                    if(isDebug){
                                        System.out.println("TraceTimeOut.java | insert await");
                                    }
                                    isInserted=true;
                                    int preoperand=codeIter.s16bitAt(address-2);
                                    String fieldName=constPool.getFieldrefName(preoperand);
                                    if(fieldName==null){
                                        bytecode.add(Opcode.DUP_X2);
                                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceAwait", "(JLjava/util/concurrent/TimeUnit;)J");
                                        bytecode.add(Opcode.DUP2_X1);
                                        bytecode.add(Opcode.POP2);
                                    }else{
                                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "traceAwait", "(JLjava/util/concurrent/TimeUnit;)J");
                                        bytecode.addGetstatic("java.util.concurrent.TimeUnit",fieldName,"Ljava/util/concurrent/TimeUnit;");
                                    }
                                }
                                codeIter.insert(address,bytecode.get());
                            }
                        }
                    }
                }
                if(isDebug){
                    printAllBytecode(codeAttribute);
                }
                codeAttribute.computeMaxStack();
            }
            // TODOEXTEND
            ctClass.writeFile(sysClassPath);
        }catch(BadBytecode bbc){
            bbc.printStackTrace();
        }catch(CannotCompileException cce){
            cce.printStackTrace();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

    }
    private static void printAllBytecode(CodeAttribute codeAttribute)throws BadBytecode{
        for(CodeIterator it=codeAttribute.iterator();it.hasNext();){
            int address=it.next();
            int opcode=it.byteAt(address);
            if(Opcode.GOTO==opcode||Opcode.IF_ICMPLT==opcode){
                int operand=it.s16bitAt(address+1);
                System.out.printf("TraceTimeOut.java | [debug] %d %s %d\n",address,Mnemonic.OPCODE[opcode],operand);
            }else{
                System.out.printf("TraceTimeOut.java | [debug] %d %s\n",address,Mnemonic.OPCODE[opcode]);
            }
        }
    }
}
