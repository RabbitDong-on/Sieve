package jsonutils;

import java.util.ArrayList;
import java.util.List;

public class DelayPoint{
    public String className;
    //subsignature
    public String funcName;
    public int lineNum;

    // for exception
    public List<String> exception=null;

    // type 1:sychronize 2: timeout 0: normal
    public int type;

    public int index=0;

    // timeout value
    public List<Long> toVList=new ArrayList<>();
}