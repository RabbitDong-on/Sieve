package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScheduleInfo implements Serializable{
    public int seqNum;

    // basic information
    public long tId;
    public String className;
    public String funcName;
    public int lineNum;

    // call stack
    public StackTraceElement[] lastSTE;
    public List<StackTraceElement[]> steList=new ArrayList<>();

    // delay time
    // 1:synchronize | 2:timeout
    public int type;
}