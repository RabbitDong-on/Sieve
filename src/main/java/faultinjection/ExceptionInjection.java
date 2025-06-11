package faultinjection;
import java.util.Iterator;

import com.alibaba.fastjson.JSON;

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
import jsonutils.DelayPoint;
import jsonutils.JsonUtil;
import jsonutils.SystemInfo;

public class ExceptionInjection{
    public String fileName;
    public String sysName;
    public String sysClassPath;
    public ExceptionInjection(String fileName,String sysName,String sysClassPath){
        this.fileName=fileName;
        this.sysName=sysName;
        this.sysClassPath=sysClassPath;
    }

    public static void main(String[] args) throws Exception{
        if(args.length!=1){
            System.out.println("ExceptionInjection | please specify system name");
            System.exit(0);
        }
        String fileName=args[0];
        String sysClassPath="/failslow/zookeeper/zookeeper-server/target/classes";
        ExceptionInjection exceptionInjection=new ExceptionInjection(fileName,args[0],sysClassPath);
        exceptionInjection.InjectException();
    }

    public void InjectException()throws Exception{
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
        int nullNum=0;
        while(iter.hasNext()){
            DelayPoint dp=iter.next();
            String className=dp.className;
            String funcName=dp.funcName;
            int lineNum=dp.lineNum;
            if(dp.exception==null){
                nullNum++;
                continue;
            }
            // System.out.println("class:"+className+"---func:"+funcName+"---line:"+lineNum);
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
                for(CodeIterator codeIter=codeAttribute.iterator();codeIter.hasNext();){
                    int address=codeIter.next();
                    int pos=methodInfo.getLineNumber(address);
                    if(lineNum==pos){
                        Bytecode bytecode=new Bytecode(constPool);
                        bytecode.addInvokestatic(FailSlowAgent.class.getName(), "exception", "()V");
                        codeIter.insert(address, bytecode.get());
                        break;
                    }
                }
                codeAttribute.computeMaxStack();
            }
            try{
                // TODOEXTEND
                success++;
                ctClass.writeFile(sysClassPath);
            }catch(CannotCompileException cce){
                fail++;
                System.out.println("ExceptionInjection | CannotCompileException class:"+className+"---func:"+funcName+"---line:"+lineNum);
            }
        }
        System.out.println("ExceptionInjection | success:"+success+"    fail:"+fail+"    nullNum:"+nullNum);
    }
}