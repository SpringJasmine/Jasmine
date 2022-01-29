package bean;

import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AOPTargetModel {
    private String className;
    private String methodName;
    private String proxyClassName;
    private String proxyMethodName;
    // target class
    private SootClass sootClass;
    // target method
    private SootMethod sootMethod;
    // proxy class
    private SootClass proxyClass;
    // proxy method
    private SootMethod proxyMethod;
    // advice
    private List<AspectModel> advices = new ArrayList<>();
    // Pointcut
    private Set<SootMethod> pointcuts = new HashSet<>();

    public AOPTargetModel() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public SootClass getSootClass() {
        return sootClass;
    }

    public void setSootClass(SootClass sootClass) {
        this.sootClass = sootClass;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public void setSootMethod(SootMethod sootMethod) {
        this.sootMethod = sootMethod;
    }

    public List<AspectModel> getAdvices() {
        return advices;
    }

    public void setAdvices(List<AspectModel> advices) {
        this.advices = advices;
    }

    public void addAdvice(AspectModel method) {
        this.advices.add(method);
    }

    public Set<SootMethod> getPointcuts() {
        return pointcuts;
    }

    public void setPointcuts(Set<SootMethod> pointcuts) {
        this.pointcuts = pointcuts;
    }

    public void addPoint(SootMethod method) {
        this.pointcuts.add(method);
    }

    public SootClass getProxyClass() {
        return proxyClass;
    }

    public void setProxyClass(SootClass proxyClass) {
        this.proxyClass = proxyClass;
    }

    public SootMethod getProxyMethod() {
        return proxyMethod;
    }

    public void setProxyMethod(SootMethod proxyMethod) {
        this.proxyMethod = proxyMethod;
    }

    public String getProxyClassName() {
        return proxyClassName;
    }

    public void setProxyClassName(String proxyClassName) {
        this.proxyClassName = proxyClassName;
    }

    public String getProxyMethodName() {
        return proxyMethodName;
    }

    public void setProxyMethodName(String proxyMethodName) {
        this.proxyMethodName = proxyMethodName;
    }

    @Override
    public String toString() {
        return "AOPTargetModel{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", proxyClassName='" + proxyClassName + '\'' +
                ", proxyMethodName='" + proxyMethodName + '\'' +
                ", sootClass=" + sootClass +
                ", sootMethod=" + sootMethod +
                ", proxyClass=" + proxyClass +
                ", proxyMethod=" + proxyMethod +
                ", advices=" + advices +
                ", pointcuts=" + pointcuts +
                '}';
    }
}
