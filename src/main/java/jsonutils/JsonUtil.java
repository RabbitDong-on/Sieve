package jsonutils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSON;


/*
 * JsonUtil
 */

public class JsonUtil{
    /*
     * writeJson
     */
    // public void writeJson(String path,String fileName,SystemInfo sysInfo){
    //     String context=JSON.toJSONString(sysInfo);
    //     // System.out.println(context);
    //     try{
    //         FileWriter writer=new FileWriter(path+"/"+fileName+".json");
    //         writer.write(context);
    //         writer.close();
    //     }catch(IOException e){
    //         e.printStackTrace();
    //     }    
    // }
    /*
     * writeJson
     */
    // public void writeJson(String path,String fileName,TOP2IO TOP2IO){
    //     String context=JSON.toJSONString(TOP2IO);
    //     // System.out.println(context);
    //     try{
    //         FileWriter writer=new FileWriter(path+"/"+fileName+".json");
    //         writer.write(context);
    //         writer.close();
    //     }catch(IOException e){
    //         e.printStackTrace();
    //     }    
    // }
    /*
     * T : different structure
     * TOP2IO, SystemInfo
     */
    public <T>void writeJson(String path,String fileName,T structure){
        String context=JSON.toJSONString(structure);
        // System.out.println(context);
        try{
            FileWriter writer=new FileWriter(path+"/"+fileName+".json");
            writer.write(context);
            writer.close();
        }catch(IOException e){
            e.printStackTrace();
        }    
    }

    /*
     * readJson
     */
    public String readJson(String path,String fileName){
        File file=new File(path+"/"+fileName+".json");
        try{
            assert file!=null;
            String context=FileUtils.readFileToString(file,"utf-8");
            return context;
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
}