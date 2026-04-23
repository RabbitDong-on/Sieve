package faultpointanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import jsonutils.JsonUtil;
import jsonutils.MemAccess;
import jsonutils.MemAccesses;

public class ParseMemAccesses {
    public boolean isDebug=false;
    public static void main(String[] args){
        ParseMemAccesses parseMemAccesses=new ParseMemAccesses();
        List<String> sList=new ArrayList<>();
        sList.add("mt");
        sList.add("dg");
        Iterator<String> sIter1=sList.iterator();
        while(sIter1.hasNext()){
            String s1=sIter1.next();
            Iterator<String> sIter2=sList.iterator();
            while(sIter2.hasNext()){
                String s2=sIter2.next();
                System.out.println("s1:"+s1+" s2:"+s2);
            }
        }
        parseMemAccesses.parseMemAccesses(args[0]);
    }
    /*
     * 1. read MemAccesses.json
     * 2. find conflicting pairs
     *    2.1 nodeId is same
     *    2.2 threadId is different
     *    2.3 address is same
     *    2.3 type is differnt
     *    2.4 both are write ops
     * 
     */
    public void parseMemAccesses(String mAFileName){
        JsonUtil jsonUtil=new JsonUtil();
        String path=System.getProperty("user.dir");
        String mAContext=jsonUtil.readJson(path, mAFileName);
        MemAccesses memAccesses=JSON.parseObject(mAContext,MemAccesses.class);
        List<MemAccess> mAList=memAccesses.memAccesses;
        System.out.println("ParseMemAccesses | mAList:"+mAList.size());
        Iterator<MemAccess> mAIter1=mAList.iterator();
        Map<MemAccess,MemAccess> conflictedPairs=new LinkedHashMap<>();
        while(mAIter1.hasNext()){
            MemAccess memAccess1=mAIter1.next();
            if(isDebug){
                System.out.println("ParseMemAccesses | type:"+memAccess1.type);
                System.out.println("ParseMemAccesses | nodeId:"+memAccess1.nodeId);
                System.out.println("ParseMemAccesses | threadId:"+memAccess1.tId);
                System.out.println("ParseMemAccesses | className:"+memAccess1.className);
                System.out.println("ParseMemAccesses | fieldName:"+memAccess1.fieldName);
                System.out.println("ParseMemAccesses | address:"+memAccess1.hashCode);
            }
            Iterator<MemAccess> mAIter2=mAList.iterator();
            while(mAIter2.hasNext()){
                MemAccess memAccess2=mAIter2.next();
                if(matchMemAccesses(memAccess1, memAccess2)){
                    conflictedPairs.put(memAccess1, memAccess2);
                }
            }
        }
        // trace timeout method & callees mAList 255298 ; conflicted pairs 19777
        System.out.println("ParseMemAccesses | conflicted pairs:"+conflictedPairs.size());
    }
    public static boolean matchMemAccesses(MemAccess mA1,MemAccess mA2){
        try{
            if(mA1.nodeId==mA2.nodeId&&
                mA1.tId!=mA2.tId&&
                mA1.className.equals(mA2.className)&&
                mA1.fieldName.equals(mA2.fieldName)&&
                mA1.hashCode==mA2.hashCode&&
                (mA1.type==1||mA2.type==1)){
                    // observe that mA1.type==1 && mA2.type==1
                    return true;
            }
        }catch(NullPointerException npe){
            npe.printStackTrace();
        }
        return false;
    }
}
