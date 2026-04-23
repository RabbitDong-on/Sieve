package faultpointanalysis;


import java.io.File;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;
import javassist.bytecode.Opcode;
import utils.*;


/*
 * TraceSharedMem
 * 1: heap object
 * 
 */
public class TraceSharedMem{
    private static boolean isDebug=false;
    public static final String sourceDir=System.getProperty("user.dir")+File.separator+"target"+File.separator+"classes";
    public static void main(String[] args){
        
    }
    /*
     * trace
     * 记录method中访问heap的地址，以及该访存操作的line，threadid
     */
    public static void trace(String className,String methodName,int startLineNum,int endLineNum,int nodeId,String sysClassPath){
        try{
            // if(className.equals("org.apache.zookeeper.ClientCnxn")&&methodName.equals("sendPacket")){
            //     System.out.println("find!");
            //     isDebug=true;
            // }
            int count=0;
            ClassPool pool=ClassPool.getDefault();
            pool.insertClassPath(sysClassPath);
            CtClass hc=pool.get(className);
            if(hc.isFrozen()){
                hc.defrost();
            }
            ClassFile classFile=hc.getClassFile();
            ConstPool constPool=classFile.getConstPool();
            for(CtBehavior ctBehavior:hc.getDeclaredBehaviors()){
                // System.out.println("TraceSharedMem | CtBehavior:"+ctBehavior);
                if(ctBehavior.isEmpty()){
                    continue;
                }
                MethodInfo methodInfo=ctBehavior.getMethodInfo();
                // System.out.println("TraceSharedMem | MethodInfo:"+methodInfo.getName());
                if(!methodInfo.getName().equals(methodName)){
                    continue;
                }
                CodeAttribute codeAttribute=methodInfo.getCodeAttribute();
                // System.out.println("TraceSharedMem | CodeAttribute:"+codeAttribute);
                if(isDebug){
                    printAllBytecode(codeAttribute);
                }
                for(CodeIterator it=codeAttribute.iterator();it.hasNext();){
                    int address=it.next();
                    int opcode=it.byteAt(address);
                    int pos=methodInfo.getLineNumber(address);
                    if((pos>=startLineNum)&&((pos<=endLineNum)||endLineNum==-1)){
                        switch(opcode){
                            case Opcode.GETSTATIC:
                                handleGetfield(it, constPool, address,methodName,pos, nodeId, 1);
                                count++;
                                break;
                            case Opcode.GETFIELD:
                                handleGetfield(it, constPool, address,methodName,pos,nodeId,2);
                                count++;
                                break;
                            case Opcode.PUTSTATIC:
                                handlePutfield(it, constPool, address, methodName,pos,nodeId,1);
                                count++;
                                break;
                            case Opcode.PUTFIELD:
                                handlePutfield(it, constPool, address,methodName,pos,nodeId,3);
                                count++;
                                break;
                        }
                    }
                }
                if(isDebug){
                    // System.out.println("TraceSharedMem | [debug] ---------- after manipulating ----------");
                    printAllBytecode(codeAttribute);
                }
                codeAttribute.computeMaxStack();
            }
            hc.writeFile(sysClassPath);
            // System.out.println("TraceSharedMem | method:"+methodName+" className:"+className+" fieldNum:"+count);
        }catch (Exception e){
            // System.out.println("TraceSharedMem | method:"+methodName+" className:"+className);
            // e.printStackTrace();
        }
    }

    private static void printAllBytecode(CodeAttribute codeAttribute)throws BadBytecode{
        for(CodeIterator it=codeAttribute.iterator();it.hasNext();){
            int address=it.next();
            int opcode=it.byteAt(address);
            if(Opcode.PUTFIELD==opcode||Opcode.PUTSTATIC==opcode||Opcode.GETFIELD==opcode||Opcode.GETSTATIC==opcode){
                int operand=it.s16bitAt(address+1);
                System.out.printf("TraceSharedMem | [debug] %d %s %d\n",address,Mnemonic.OPCODE[opcode],operand);
            }else{
                if(Opcode.GOTO==opcode||Opcode.IF_ICMPLT==opcode){
                    int operand=it.s16bitAt(address+1);
                    System.out.printf("TraceSharedMem | [debug] %d %s %d\n",address,Mnemonic.OPCODE[opcode],operand);
                }else{
                    System.out.printf("TraceSharedMem | [debug] %d %s\n",address,Mnemonic.OPCODE[opcode]);
                }
            }
        }
    }

    // type 1: static RW 2: non-static R 3: non-static W
    private static void handleGetfield(CodeIterator it,ConstPool constPool,int address,String funcName,int lineNum,int nodeId,int type)throws BadBytecode{
        Bytecode bytecode=new Bytecode(constPool);
        String fieldType=handleBytecodeForField(bytecode,it,constPool,address,type);
        bytecode.addLdc(funcName);
        bytecode.addLdc2w((long)lineNum);
        if(nodeId==1){
            bytecode.add(Opcode.ICONST_1);
        }else if(nodeId==2){
            bytecode.add(Opcode.ICONST_2);
        }else if(nodeId==3){
            bytecode.add(Opcode.ICONST_3);
        }else if(nodeId==4){
            bytecode.add(Opcode.ICONST_4);
        }
        if(type==1){
            // static read
            bytecode.addInvokestatic(MemoryTraceLogUtils.class.getName(),"traceFieldRead", String.format("(%sLjava/lang/String;Ljava/lang/String;Ljava/lang/String;JI)V",fieldType));
            it.insert(bytecode.get());
        }else{
            // non-static read
            bytecode.addInvokestatic(MemoryTraceLogUtils.class.getName(),"traceNSFieldRead", String.format("(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JI)V"));
            it.insert(address, bytecode.get());
        }
    }

    private static void handlePutfield(CodeIterator it, ConstPool constPool, int address,String funcName,int lineNum,int nodeId, int type) throws BadBytecode {
        Bytecode bytecode = new Bytecode(constPool);
        String fieldType = handleBytecodeForField(bytecode, it, constPool, address,type);
        // funcName
        bytecode.addLdc(funcName);
        // lineNum
        bytecode.addLdc2w((long)lineNum);
        if(nodeId==1){
            bytecode.add(Opcode.ICONST_1);
        }else if(nodeId==2){
            bytecode.add(Opcode.ICONST_2);
        }else if(nodeId==3){
            bytecode.add(Opcode.ICONST_3);
        }else if(nodeId==4){
            bytecode.add(Opcode.ICONST_4);
        }
        if(type==1){
            // static write
            bytecode.addInvokestatic(MemoryTraceLogUtils.class.getName(),"traceFieldWrite", String.format("(%sLjava/lang/String;Ljava/lang/String;Ljava/lang/String;JI)V", fieldType));
        }else{
            // non-static write
            bytecode.addInvokestatic(MemoryTraceLogUtils.class.getName(),"traceNSFieldWrite", String.format("(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JI)V"));
        }
        it.insert(address, bytecode.get());
    }

    private static String handleBytecodeForField(Bytecode bytecode,CodeIterator it,ConstPool constPool,int address,int type){
        int operand=it.s16bitAt(address+1);
        String fieldName=constPool.getFieldrefName(operand);
        // System.out.println("TraceSharedMem | field:"+fieldName);
        String fieldClassName=constPool.getFieldrefClassName(operand);
        // System.out.println("TraceSharedMem | fieldClass:"+fieldClassName);
        String fieldType=constPool.getFieldrefType(operand);
        fieldType=fieldType.length()>1?"Ljava/lang/Object;":fieldType;
        // System.out.println("TraceSharedMem | fieldType:"+fieldType);
        if(type==1){
            // static
            if("J".equals(fieldType)||"D".equals(fieldType)){
                bytecode.add(Opcode.DUP2);
            }else{
                bytecode.add(Opcode.DUP);
            }
        }else if(type==2){
            // non-static R
            bytecode.add(Opcode.DUP);
        }else if(type==3){
            // non-static W
            // cannot solve uninitializedthis
            if("J".equals(fieldType)||"D".equals(fieldType)){
                // L/D|obj
                bytecode.add(Opcode.DUP2_X1);
                // L/D|obj|L/D
                bytecode.add(Opcode.POP2);
                // obj|L/D
                bytecode.add(Opcode.DUP_X2);
                // obj|L/D|obj
                // bytecode.add(Opcode.DUP_X2);
                // obj|L/D|obj|obj
                // bytecode.add(Opcode.POP);
                // L/D|obj|obj
                // bytecode.add(Opcode.DUP2_X1);
                // L/D|obj|L/D|obj
            }else{
                // 1|obj
                bytecode.add(Opcode.DUP2);
                // 1|obj|1|obj
                bytecode.add(Opcode.POP);
                // obj|1|obj
            }
        }
        bytecode.addLdc(fieldClassName);
        bytecode.addLdc(fieldName);
        return fieldType;
    }
}
