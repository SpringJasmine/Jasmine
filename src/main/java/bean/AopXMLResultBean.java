package bean;

public class AopXMLResultBean {
    // aspect
    private String aopclass;
    // advice
    private String aopmethod;
    // advice type
    private String activetype;
    // pointcut expression
    private String exper;
    // order
    private int order;

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public AopXMLResultBean() {
    }

    public AopXMLResultBean(String aopclass, String aopmethod, String activetype, String exper, int order) {
        this.aopclass = aopclass;
        this.aopmethod = aopmethod;
        this.activetype = activetype;
        this.exper = exper;
        this.order = order;
    }

    @Override
    public String toString() {
        return "AopXMLResultBean{" +
                "aopclass='" + aopclass + '\'' +
                ", aopmethod='" + aopmethod + '\'' +
                ", activetype='" + activetype + '\'' +
                ", exper='" + exper + '\'' +
                ", order=" + order +
                '}';
    }

    public String getAopclass() {
        return aopclass;
    }

    public void setAopclass(String aopclass) {
        this.aopclass = aopclass;
    }

    public String getAopmethod() {
        return aopmethod;
    }

    public void setAopmethod(String aopmethod) {
        this.aopmethod = aopmethod;
    }

    public String getActivetype() {
        return activetype;
    }

    public void setActivetype(String activetype) {
        this.activetype = activetype;
    }

    public String getExper() {
        return exper;
    }

    public void setExper(String exper) {
        this.exper = exper;
    }
}
