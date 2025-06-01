package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/*
 * ReadIOClass
 */
public class ReadIOClass{
    String sourceDir=System.getProperty("user.dir");
    public static List<String> readIOClass(String filename){
        File file=new File(filename);
        List<String> res=new ArrayList<>();
        try{
            Scanner reader=new Scanner(file);
            while(reader.hasNextLine()){
                String data=reader.nextLine();
                res.add(data);
            }
            reader.close();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        return res;
    }
}