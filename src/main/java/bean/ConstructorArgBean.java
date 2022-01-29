package bean;


public class ConstructorArgBean {
    private String argName;
    private Integer argIndex;
    private String argType;
    private String refType;
    private String argValue;
    private String clazzName;

    @Override
    public String toString() {
        return "ConstructorArgBean{" +
                "argName='" + argName + '\'' +
                ", argIndex=" + argIndex +
                ", argType='" + argType + '\'' +
                ", refType='" + refType + '\'' +
                ", argValue='" + argValue + '\'' +
                ", clazzName='" + clazzName + '\'' +
                '}';
    }

    public String getArgName() {
        return argName;
    }

    public void setArgName(String argName) {
        this.argName = argName;
    }

    public Integer getArgIndex() {
        return argIndex;
    }

    public void setArgIndex(Integer argIndex) {
        this.argIndex = argIndex;
    }

    public String getArgType() {
        return argType;
    }

    public void setArgType(String argType) {
        this.argType = argType;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public String getArgValue() {
        return argValue;
    }

    public void setArgValue(String argValue) {
        this.argValue = argValue;
    }

    public String getClazzName() {
        return clazzName;
    }

    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }
}
