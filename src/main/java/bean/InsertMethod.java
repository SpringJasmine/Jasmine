package bean;

import soot.SootMethod;

import java.util.List;


public class InsertMethod {
    private SootMethod sootMethod;
    private List<Integer> returnList;
    private List<Integer> insertPointList;
    private List<Integer> pjpList;

    public InsertMethod() {
    }

    public InsertMethod(SootMethod sootMethod, List<Integer> returnList) {
        this.sootMethod = sootMethod;
        this.returnList = returnList;
    }

    public InsertMethod(SootMethod sootMethod, List<Integer> returnList, List<Integer> insertPointList) {
        this.sootMethod = sootMethod;
        this.returnList = returnList;
        this.insertPointList = insertPointList;
    }

    public InsertMethod(SootMethod sootMethod, List<Integer> returnList, List<Integer> insertPointList, List<Integer> pjpList) {
        this.sootMethod = sootMethod;
        this.returnList = returnList;
        this.insertPointList = insertPointList;
        this.pjpList = pjpList;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public void setSootMethod(SootMethod sootMethod) {
        this.sootMethod = sootMethod;
    }

    public List<Integer> getReturnList() {
        return returnList;
    }

    public void setReturnList(List<Integer> returnList) {
        this.returnList = returnList;
    }

    public List<Integer> getInsertPointList() {
        return insertPointList;
    }

    public void setInsertPointList(List<Integer> insertPointList) {
        this.insertPointList = insertPointList;
    }

    public List<Integer> getPjpList() {
        return pjpList;
    }

    public void setPjpList(List<Integer> pjpList) {
        this.pjpList = pjpList;
    }

    @Override
    public String toString() {
        return "InsertMethod{" +
                "sootMethod=" + sootMethod +
                ", returnList=" + returnList +
                ", insertPointList=" + insertPointList +
                ", pjpList=" + pjpList +
                '}';
    }
}
