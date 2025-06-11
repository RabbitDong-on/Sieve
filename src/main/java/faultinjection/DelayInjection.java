package faultinjection;
import com.alibaba.fastjson.JSON;

import client.FailSlowAgent;
import javassist.*;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import jsonutils.*;
import java.util.Iterator;



public class DelayInjection{
    public String fileName;
    public String sysName;
    public String sysClassPath;
    public DelayInjection(String fileName,String sysName,String sysClassPath){
        this.fileName=fileName;
        this.sysName=sysName;
        this.sysClassPath=sysClassPath;
    }
    public static void main(String[] args) throws Exception{
        if(args.length!=1){
            System.out.println("DelayInjection | please specify system name");
            System.exit(0);
        }
        String fileName=args[0]+"TimeOutSync";
        int nodeId=Integer.parseInt(args[1]);
        String sysClassPath="/failslow/zookeeper/zookeeper-server/target/classes";
        DelayInjection delayInjection=new DelayInjection(fileName,args[0],sysClassPath);
        delayInjection.InjectDelay(nodeId);
    }
    public void InjectDelay(int nodeId)throws Exception{
        JsonUtil jsonUtil=new JsonUtil();
        // String context=jsonUtil.readJson(System.getProperty("user.dir"), fileName);
        String path=System.getProperty("user.dir");
        String context=jsonUtil.readJson(path, fileName);
        SystemInfo systemInfo=JSON.parseObject(context,SystemInfo.class);

        ClassPool pool=ClassPool.getDefault();
        // TODOEXTEND
        pool.insertClassPath(sysClassPath);
        Iterator<DelayPoint> iter=systemInfo.dpSet.iterator();
        int success=0;
        int fail=0;
        while(iter.hasNext()){
            DelayPoint dp=iter.next();
            String className=dp.className;
            String funcName=dp.funcName;
            int lineNum=dp.lineNum;
            int type=dp.type;
            // boolean isDebug=false;
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
                // if(funcName.contains("storeOffsets")){
                //     System.out.println("---- | lineNum:"+lineNum);
                //     System.out.println("---- | funcName:"+funcName);
                //     System.out.println("---- | className:"+className);
                //     isDebug=true;
                // }
                CodeAttribute codeAttribute=methodInfo.getCodeAttribute();
                for(CodeIterator codeIter=codeAttribute.iterator();codeIter.hasNext();){
                    int address=codeIter.next();
                    int pos=methodInfo.getLineNumber(address);
                    // if(isDebug){
                    //     System.out.println("------ | find:"+pos+"  lineNum:"+lineNum);
                    // }
                    if(lineNum==pos){
                        Bytecode bytecode=new Bytecode(constPool);
                        if(type==0){
                            bytecode.add(Opcode.ICONST_0);
                        }else if(type==1){
                            bytecode.add(Opcode.ICONST_1);
                        }else if(type==2){
                            bytecode.add(Opcode.ICONST_2);
                        }
                        if(nodeId==1){
                            bytecode.add(Opcode.ICONST_1);
                        }else if(nodeId==2){
                            bytecode.add(Opcode.ICONST_2);
                        }else if(nodeId==3){
                            bytecode.add(Opcode.ICONST_3);
                        }else if(nodeId==4){
                            bytecode.add(Opcode.ICONST_4);
                        }
                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "report", "(II)V");
                        codeIter.insert(address, bytecode.get());
                        break;
                    }
                }
                codeAttribute.computeMaxStack();
            }
            try{
                // TODOEXTEND
                ctClass.writeFile(sysClassPath);
                success++;
            }catch(CannotCompileException cce){
                fail++;
                System.out.println("DelayInjection.java | CannotCompileException class:"+className+"---func:"+funcName+"---line:"+lineNum);
            }
        }
        System.out.println("DelayInjection.java | success:"+success+"    fail:"+fail);
    }
}