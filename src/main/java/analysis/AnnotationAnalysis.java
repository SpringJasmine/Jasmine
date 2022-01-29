package analysis;

import soot.*;
import soot.tagkit.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @ClassName AnnotationAnalysis
 * @Description Processing of annotations
 **/
public class AnnotationAnalysis {
    private static volatile AnnotationAnalysis INSTANCE = null;
    public static List<Type> autoMethodParams = new ArrayList<>();
    public static Set<String> mapperPackages = new HashSet<>();
    public static Set<SootClass> controllers = new HashSet<>();

    public AnnotationAnalysis() {
    }

    public static AnnotationAnalysis getInstance() {
        if (INSTANCE == null) {
            synchronized (AnnotationAnalysis.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AnnotationAnalysis();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Getting and returning all the fields of this class according to the class name
     *
     * @param className class name
     * @return fields
     */
    public Chain<SootField> getClassFields(String className) {
        SootClass sootClass = Scene.v().getSootClass(className);
        return sootClass.getFields();
    }

    /**
     * Getting all annotations on the field
     *
     * @param field field
     * @return the list of annotations on the field
     */
    public List<Tag> getFieldTags(SootField field) {
        return field.getTags();
    }

    /**
     * Getting fields with inject annotations, such as @Autowired, etc.
     *
     * @param field field
     * @return If these special annotations are included, return the field. Otherwise,
     * return null
     */
    public SootField getFieldWithSpecialAnnos(SootField field, SootMethod initMethod, boolean ambiguous) {
        List<Tag> fieldTags = getFieldTags(field);
        for (Tag fieldTag : fieldTags) {
            String strtag = fieldTag.toString();
            if (strtag.contains("Autowired")
                    || strtag.contains("Qualifier")
                    || strtag.contains("Resource")
                    || strtag.contains("Inject")) {
                return field;
            }
        }
        if (autoMethodParams.contains(field.getType())) {
            return field;
        }
        if (!ambiguous && initMethod.getParameterTypes().contains(field.getType())) {
            return field;
        }
        return null;
    }

    public List<Type> getParamOfAutoWiredMethod(SootMethod method) {
        VisibilityAnnotationTag methodTag = (VisibilityAnnotationTag) method.getTag("VisibilityAnnotationTag");
        List<Type> parameterTypes = null;
        if (methodTag != null) {
            for (AnnotationTag annotation : methodTag.getAnnotations()) {
                if (annotation.getType().contains("Autowired")) {
                    parameterTypes = method.getParameterTypes();
                    break;
                }
            }
        }
        return parameterTypes;
    }

    /**
     * According to the @ComponentScan annotation, function scan those classes need
     * to be initialized.
     *
     * @param ApplicationClass The startup class of the Spring program
     */
    public void findComponents(SootClass ApplicationClass) {
        AnnotationTag annotationScan = hasSpecialAnnotation(ApplicationClass);
        if (annotationScan != null) {
            for (AnnotationElem elem : annotationScan.getElems()) {
                if (elem instanceof AnnotationArrayElem && elem.getName().equals("basePackages")) {
                    AnnotationArrayElem arrayElem = (AnnotationArrayElem) elem;
                    for (AnnotationElem value : arrayElem.getValues()) {
                        if (value instanceof AnnotationStringElem) {
                            AnnotationStringElem stringElem = (AnnotationStringElem) value;
                            CreateEdge.componentPackages.add(stringElem.getValue());
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Get the annotation information that will be included in all classes that need
     * to be initialized as Bean.
     *
     * @param sootClass Class represented by soot
     * @return Whether to include the corresponding annotations, 0 means not included,
     * 1 means the class is a bean, 1+2 means the class uses the prototype pattern Bean,
     * 1+4 (4) means a Mapper, 1+2+4 (2+4) Mapper for prototype pattern.
     */
    public Integer getAllComponents(SootClass sootClass) {
        VisibilityAnnotationTag annotationTags = (VisibilityAnnotationTag) sootClass.getTag("VisibilityAnnotationTag");
        int flag = 0;
        if (annotationTags != null) {
            for (AnnotationTag annotation : annotationTags.getAnnotations()) {
                switch (annotation.getType()) {
                    case "Lorg/springframework/web/bind/annotation/RestController;":
                    case "Lorg/springframework/web/bind/annotation/Controller;":
                    case "Lorg/springframework/stereotype/Controller;":
                        controllers.add(sootClass);
                        if (!SpringAnnotationTag.isBean(flag)) {
                            flag += SpringAnnotationTag.BEAN;
                        }
                        break;
                    case "Lorg/apache/ibatis/annotations/Mapper;":
                    case "Lorg/beetl/sql/core/annotatoin/SqlResource;":
                        if (!SpringAnnotationTag.isMapper(flag)) {
                            flag += SpringAnnotationTag.MAPPER;
                        }
                    case "Lorg/springframework/stereotype/Component;":
                    case "Lorg/springframework/context/annotation/Configuration;":
                    case "Lorg/springframework/stereotype/Repository;":
                    case "Lorg/springframework/stereotype/Service;":
                        if (!SpringAnnotationTag.isBean(flag)) {
                            flag += SpringAnnotationTag.BEAN;
                        }
                        break;
                    case "Lorg/springframework/context/annotation/Scope;":
                        for (AnnotationElem elem : annotation.getElems()) {
                            AnnotationStringElem stringElem = (AnnotationStringElem) elem;
                            if (stringElem.getValue().equals("prototype") && !SpringAnnotationTag.isPrototype(flag)) {
                                flag += SpringAnnotationTag.PROTOTYPE;
                            }
                        }
                        break;
                    case "Lorg/mybatis/spring/annotation/MapperScan;":
                        for (AnnotationElem elem : annotation.getElems()) {
                            AnnotationArrayElem arrayElem = (AnnotationArrayElem) elem;
                            for (AnnotationElem value : arrayElem.getValues()) {
                                AnnotationStringElem asElem = (AnnotationStringElem) value;
                                mapperPackages.add(asElem.getValue());
                            }
                        }
                        break;
                }
            }
        }
        return flag;
    }

    /**
     * Get all methods that contain @Bean annotations that need to be initialized as bean
     *
     * @param sootClass Class represented by soot
     * @return List of methods that need to be initialized as bean
     */
    public Set<SootMethod> getAllBeans(SootClass sootClass) {
        Set<SootMethod> allBeans = new HashSet<>();
        for (SootMethod method : sootClass.getMethods()) {
            VisibilityAnnotationTag annotationTags = (VisibilityAnnotationTag) method.getTag("VisibilityAnnotationTag");
            if (annotationTags != null) {
                for (AnnotationTag annotation : annotationTags.getAnnotations()) {
                    if (annotation.getType().equals("Lorg/springframework/context/annotation/Bean;")) {
                        allBeans.add(method);
                        SootClass bean = Scene.v().getSootClass(method.getReturnType().toString());
                        if (bean.isApplicationClass() && !bean.isAbstract()) {
                            CreateEdge.singletonComponents.add(bean);
                            CreateEdge.interfaceToBeans.put(bean.getName(), bean);
                        }
                    }
                }
            }
        }
        return allBeans;
    }

    /**
     * find whether there is a specified annotation on the specified domain (class, method, field)
     *
     * @param host Specify the domain (class, method, member variable)
     * @return Whether to have specified annotations
     */
    public AnnotationTag hasSpecialAnnotation(AbstractHost host) {
        VisibilityAnnotationTag annotationTags = (VisibilityAnnotationTag) host.getTag("VisibilityAnnotationTag");
        if (annotationTags == null) {
            return null;
        }
        for (AnnotationTag annotation : annotationTags.getAnnotations()) {
            if (satisfyAnnotation(annotation.getType())) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * find whether the annotation meets the entry point condition---add conditions
     * as needed in the followup.
     */
    private static boolean satisfyAnnotation(String type) {
        switch (type) {
            case "Lorg/springframework/web/bind/annotation/RequestMapping;":
            case "Lorg/springframework/web/bind/annotation/PostMapping;":
            case "Lorg/springframework/web/bind/annotation/GetMapping;":
            case "Lorg/springframework/web/bind/annotation/PatchMapping;":
            case "Lorg/springframework/web/bind/annotation/DeleteMapping;":
            case "Lorg/springframework/web/bind/annotation/PutMapping;":
            case "Lorg/springframework/web/bind/annotation/RestController;":
            case "Lorg/springframework/web/bind/annotation/Controller;":
            case "Lorg/springframework/stereotype/Controller;":
                return true;
            default:
                return false;
        }
    }


}
