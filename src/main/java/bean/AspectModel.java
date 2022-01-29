package bean;

import enums.AdviceEnum;
import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * entity class which contains to advices
 */
public class AspectModel implements Comparable<AspectModel> {
    // The aspect of advice
    private SootClass sootClass;
    // @Order value
    private int order;
    // pointcut expression
    private List<String> pointcutExpressions = new ArrayList<>();
    // advice
    private SootMethod sootMethod;
    // the type of advice(@Around, @Before, @After)
    private AdviceEnum annotation;

    public AspectModel(SootClass sootClass, int order) {
        this.sootClass = sootClass;
        this.order = order;
    }

    public AspectModel(SootClass sootClass, int order, SootMethod sootMethod) {
        this.sootClass = sootClass;
        this.order = order;
        this.sootMethod = sootMethod;
    }

    public AspectModel() {
    }

    public SootClass getSootClass() {
        return sootClass;
    }

    public void setSootClass(SootClass sootClass) {
        this.sootClass = sootClass;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setPointcutExpressions(List<String> pointcutExpressions) {
        this.pointcutExpressions = pointcutExpressions;
    }

    /**
     * add pointcut expression
     *
     * @param pointcutExpression pointcut expression
     */
    public void addPointcutExpressions(String pointcutExpression) {
        this.pointcutExpressions.add(pointcutExpression);
    }

    public List<String> getPointcutExpressions() {
        return pointcutExpressions;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public void setSootMethod(SootMethod sootMethod) {
        this.sootMethod = sootMethod;
    }

    public AdviceEnum getAnnotation() {
        return annotation;
    }

    public void setAnnotation(AdviceEnum annotation) {
        this.annotation = annotation;
    }

    /**
     * According to the order value, sort from largest to smallest. The smaller
     * the order value, the higher the priority. If the order is the same, it
     * will be sorted by name, and then sorted by advice type.
     *
     * @param o Priority comparison object
     * @return
     */
    @Override
    public int compareTo(AspectModel o) {
        if (o.order - this.order > 0) {
            return -1;
        } else if (o.order - this.order < 0) {
            return 1;
        } else if (this.getSootClass().getName().compareTo(o.getSootClass().getName()) > 0) {
            return 1;
        } else if (this.getSootClass().getName().compareTo(o.getSootClass().getName()) < 0) {
            return -1;
        } else {
            return this.getAnnotation().ordinal() - o.getAnnotation().ordinal();
        }
    }

    @Override
    public String toString() {
        return "AspectModel{" +
                "sootClass=" + sootClass +
                ", order=" + order +
                ", pointcutExpression='" + pointcutExpressions + '\'' +
                ", sootMethod=" + sootMethod +
                ", annotation='" + annotation + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AspectModel that = (AspectModel) o;
        return order == that.order &&
                Objects.equals(sootClass, that.sootClass) &&
                Objects.equals(pointcutExpressions, that.pointcutExpressions) &&
                Objects.equals(sootMethod, that.sootMethod) &&
                annotation == that.annotation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootClass, order, pointcutExpressions, sootMethod, annotation);
    }
}
