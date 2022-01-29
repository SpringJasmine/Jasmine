package enums;

public enum AdviceEnum implements EnumMessage {
    AOP_AROUND("Lorg/aspectj/lang/annotation/Around;", "@Around"),
    AOP_BEFORE("Lorg/aspectj/lang/annotation/Before;", "@Before"),
    AOP_AFTER_RETURNING("Lorg/aspectj/lang/annotation/AfterReturning;", "@AfterReturning"),
    AOP_AFTER_THROWING("Lorg/aspectj/lang/annotation/AfterThrowing;", "@AfterThrowing"),
    AOP_AFTER("Lorg/aspectj/lang/annotation/After;", "@After");


    private String annotationClassName;
    private String annotation;

    AdviceEnum(String annotationClassName, String annotation) {
        this.annotationClassName = annotationClassName;
        this.annotation = annotation;
    }

    public String getAnnotationClassName() {
        return annotationClassName;
    }

    public void setAnnotationClassName(String annotationClassName) {
        this.annotationClassName = annotationClassName;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }


    @Override
    public Object getValue() {
        return annotationClassName;
    }
}
