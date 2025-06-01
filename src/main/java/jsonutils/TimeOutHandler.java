package jsonutils;


public class TimeOutHandler {
    public String className;
    public String funcName;
    public int lineNum;
    @Override
    public boolean equals(Object obj){
        if(obj instanceof TimeOutHandler){
            TimeOutHandler toHandler=(TimeOutHandler)obj;
            if(this.className.equals(toHandler.className)&&this.funcName.equals(toHandler.funcName)&&this.lineNum==toHandler.lineNum){
                return true;
            }
        }
        return false;
    }
}
