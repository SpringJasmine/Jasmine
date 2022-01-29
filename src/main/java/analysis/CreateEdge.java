package analysis;

import bean.AOPTargetModel;
import bean.AopXMLResultBean;
import bean.AspectModel;
import bean.ConstructorArgBean;
import enums.AdviceEnum;
import mock.GenerateSyntheticClass;
import mock.GenerateSyntheticClassImpl;
import org.dom4j.Document;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JReturnVoidStmt;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationStringElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.Chain;
import utils.EnumUtils;
import utils.FileUtils;
import utils.JimpleUtils;
import utils.XMLDocumentHolder;

import java.util.*;
import java.util.stream.Collectors;

public class CreateEdge {
    // List of packages where the classes that need to be Bean initialization are located
    public static Set<String> componentPackages = new HashSet<>();
    // The class that analyzes the corresponding annotation
    private final AnnotationAnalysis annotationAnalysis = new AnnotationAnalysis();
    private final IOCParser iocParser = new IOCParser();
    private final AOPAnalysis aopAnalysis = new AOPAnalysis();
    // List of methods that need to be initialized as Bean
    private final Set<SootMethod> allBeans = new HashSet<>();
    public static final Set<SootClass> allBeansAndInterfaces = new HashSet<>();
    // Get all classes defined as singleton beans
    public static final Set<SootClass> singletonComponents = new HashSet<>();
    // Get all classes defined as prototype beans
    public static final Set<SootClass> prototypeComponents = new HashSet<>();
    public static final Map<String, SootClass> interfaceToBeans = new HashMap<>();
    public static Chain<SootClass> sootClassChain = Scene.v().getApplicationClasses();
    public static Map<String, AspectModel> aspectModelMap = new HashMap<>();
    private final JimpleUtils jimpleUtils = new JimpleUtils();
    protected String dummyClassName = "synthetic.method.dummyMainClass";
    public SootMethod projectMainMethod;

    public void initCallGraph(String configPath) {
        try {
            // Scan the entire project, get and classify all beans
            scanAllBeans(configPath);
            // Scan the entire project, get and classify all beans
            aspectAnalysis(configPath);
            // Handling aspect-oriented programming (XML)
            //xmlaspectAnalysis(configPath);
            // Handling dependency injection logic
            dependencyInjectAnalysis();
            // Build entry points
            generateEntryPoints();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    /**
     * Scan the entire project to get all beans
     *
     * @param configPath  The path of the xml configuration file, it can be multiple, pass in a set collection to store multiple xml paths
     */
    private void scanAllBeans(String configPath) {
        Set<String> bean_xml_paths = FileUtils.getBeanXmlPaths(configPath, "bean_xml_paths");
        if (bean_xml_paths.size() > 0) {
            Set<XmlBeanClazz> xmlBeanSootClazzes = getXMLBeanSootClazzes(bean_xml_paths);
            for (XmlBeanClazz xmlBeanSootClazz : xmlBeanSootClazzes) {
                for (SootClass anInterface : xmlBeanSootClazz.getSootClass().getInterfaces()) {
                    interfaceToBeans.put(anInterface.getName(), xmlBeanSootClazz.getSootClass());
                }
                interfaceToBeans.put(xmlBeanSootClazz.getSootClass().getName(), xmlBeanSootClazz.getSootClass());
                if (xmlBeanSootClazz.getScope().equals("singleton")) {
                    singletonComponents.add(xmlBeanSootClazz.getSootClass());
                } else if (xmlBeanSootClazz.getScope().equals("prototype")) {
                    prototypeComponents.add(xmlBeanSootClazz.getSootClass());
                }
            }
        }

        XMLDocumentHolder xmlHolder = getXMLHolder(bean_xml_paths);
        Map<String, List<ConstructorArgBean>> collect = null;
        if (xmlHolder != null) {
            List<ConstructorArgBean> argConstructors = xmlHolder.getArgConstructors();
            collect = argConstructors.stream().collect(Collectors.groupingBy(ConstructorArgBean::getClazzName, Collectors.toList()));
        }
        GenerateSyntheticClass gsc = new GenerateSyntheticClassImpl();
        Collection<SootClass> elementsUnsorted = sootClassChain.getElementsUnsorted();
        Iterator<SootClass> iterator = elementsUnsorted.iterator();
        Set<SootClass> prototypeInterfaces = new HashSet<>();
        while (iterator.hasNext()) {
            SootClass sootClass = iterator.next();
            allBeans.addAll(annotationAnalysis.getAllBeans(sootClass));
            int annotationTag = annotationAnalysis.getAllComponents(sootClass);
            if (SpringAnnotationTag.isBean(annotationTag)) {
                SootClass implClass = sootClass;
                allBeansAndInterfaces.add(implClass);
                if (SpringAnnotationTag.isMapper(annotationTag)) {
                    implClass = gsc.generateMapperImpl(sootClass);
                }
                findAllSuperclass(implClass, implClass);
                interfaceToBeans.put(implClass.getName(), implClass);

                if (SpringAnnotationTag.isPrototype(annotationTag)) {
                    prototypeComponents.add(implClass);
                } else {
                    singletonComponents.add(implClass);
                }
            } else {
                if (SpringAnnotationTag.isPrototype(annotationTag)) {
                    prototypeInterfaces.add(sootClass);
                }
            }
        }

        Iterator<SootClass> iteratorMapper = elementsUnsorted.iterator();
        Hierarchy hierarchy = new Hierarchy();
        Set<SootClass> databaseModels = new HashSet<>();
        while (iteratorMapper.hasNext()) {
            SootClass sootClass = iteratorMapper.next();
            if (!sootClass.isInterface() || hierarchy.getImplementersOf(sootClass).size() > 0 || sootClass.isPhantom()) {
                continue;
            }
            boolean impl = false;
            for (String mapperPackage : AnnotationAnalysis.mapperPackages) {
                if (sootClass.getPackageName().startsWith(mapperPackage)) {
                    databaseModels.add(sootClass);
                    impl = true;
                    break;
                }
            }
            if (!impl) {
                for (SootClass anInterface : sootClass.getInterfaces()) {
                    if (anInterface.getPackageName().startsWith("org.springframework.data")
                            || anInterface.getName().equals("com.baomidou.mybatisplus.core.mapper.BaseMapper")) {
                        databaseModels.add(sootClass);
                        impl = true;
                        break;
                    }
                }
            }
            if (!impl) {
                if (sootClass.getName().toLowerCase().endsWith("dao")
                        || sootClass.getName().toLowerCase().endsWith("mapper")
                        || sootClass.getName().toLowerCase().endsWith("repository")) {
                    databaseModels.add(sootClass);
                }
            }
        }
        for (SootClass databaseModel : databaseModels) {
            implMapper(gsc, prototypeInterfaces, databaseModel);
        }
        gsc.generateSingletonBeanFactory(singletonComponents, collect);
    }

    private void implMapper(GenerateSyntheticClass gsc, Set<SootClass> prototypeInterfaces, SootClass sootClass) {
        SootClass mapperImplClass = gsc.generateMapperImpl(sootClass);
        interfaceToBeans.put(sootClass.getName(), mapperImplClass);
        allBeansAndInterfaces.add(sootClass);
        if (prototypeInterfaces.contains(sootClass)) {
            prototypeComponents.add(mapperImplClass);
        } else {
            singletonComponents.add(mapperImplClass);
        }
    }

    private void findAllSuperclass(SootClass superclass, SootClass implClass) {
        for (SootClass anInterface : superclass.getInterfaces()) {
            interfaceToBeans.put(anInterface.getName(), implClass);
            allBeansAndInterfaces.add(anInterface);
        }
        if (superclass.hasSuperclass() && !superclass.getSuperclass().getName().equals("java.lang.Object")) {
            findAllSuperclass(superclass.getSuperclass(), implClass);
        }
    }

    /**
     * Logic for handling dependency injection
     */
    private void dependencyInjectAnalysis() {
        for (SootClass sootClass : allBeansAndInterfaces) {
            if (sootClass.getName().contains("SingletonFactory")
                    || sootClass.getName().startsWith("synthetic.")
                    || sootClass.isInterface()) {
                continue;
            }
            iocParser.getIOCObject(sootClass, allBeans);
        }
    }

    private void generateEntryPoints() {
        SootMethod psm = findMainMethod();
        if (psm == null) {
            SootClass sClass = new SootClass(dummyClassName, Modifier.PUBLIC);
            sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
            Scene.v().addClass(sClass);
            sClass.setApplicationClass();
            SootMethod mainMethod = new SootMethod("main",
                    Arrays.asList(new Type[]{ArrayType.v(RefType.v("java.lang.String"), 1)}),
                    VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
            sClass.addMethod(mainMethod);
            JimpleBody jimpleBody = createJimpleBody(mainMethod);
            mainMethod.setActiveBody(jimpleBody);
            psm = mainMethod;
        }
        for (SootClass controller : AnnotationAnalysis.controllers) {
            linkMainAndController(controller, psm);
        }
        projectMainMethod = psm;
    }

    private SootMethod findMainMethod() {
        Collection<SootClass> elementsUnsorted = sootClassChain.getElementsUnsorted();
        for (SootClass sootClass : elementsUnsorted) {
            List<SootMethod> sootMethods = sootClass.getMethods();
            if (sootMethods.size() > 1) {
                for (SootMethod sootMethod : sootMethods) {
                    if (sootMethod.getSubSignature().contains("void main") && sootMethod.isStatic()) {
                        for (Unit unit : sootMethod.retrieveActiveBody().getUnits()) {
                            if (unit.toString().contains("SpringApplication")) {
                                return sootMethod;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private JimpleBody createJimpleBody(SootMethod method) {
        // Create a body for the main method and set it as the active body
        JimpleBody body = Jimple.v().newBody(method);

        // Create a local to hold the main method argument
        // Note: In general for any use of objects or basic-types, must generate a local to
        // hold that in the method body
        Local frm1 = Jimple.v().newLocal("frm1", ArrayType.v(RefType.v("java.lang.String"), 1));
        body.getLocals().add(frm1);

        // Create a unit (or statement) that assigns main's formal param into the local arg
        PatchingChain<Unit> units = body.getUnits();
        units.add(Jimple.v().newIdentityStmt(frm1,
                Jimple.v().newParameterRef(ArrayType.v
                        (RefType.v("java.lang.String"), 1), 0)));

        units.add(Jimple.v().newReturnVoidStmt());
        return body;
    }


    /**
     * Construct a callEntry function for each controller class to call private methods (useful in spark)
     *
     * @param controller  The class need to add callEntry method
     * @param psm        main method
     */
    private void linkMainAndController(SootClass controller, SootMethod psm) {
        List<SootMethod> signatures = new ArrayList<>();
        SootClass sootClass = AOPParser.proxyMap.getOrDefault(controller.getName(), controller);
        SootMethod createMethod = new SootMethod("callEntry_synthetic",
                null,
                VoidType.v(), Modifier.PUBLIC);
        sootClass.addMethod(createMethod);
        for (SootMethod method : controller.getMethods()) {

            AnnotationTag requestMapping = annotationAnalysis.hasSpecialAnnotation(method);
            if (requestMapping != null) {
                if (sootClass == controller) {
                    signatures.add(method);
                } else {
                    SootMethod proxyMethod = sootClass.getMethodUnsafe(method.getSubSignature());
                    if (proxyMethod != null) {
                        signatures.add(proxyMethod);
                    }
                }
            }
        }
        JimpleBody jimpleBody = jimpleUtils.createJimpleBody(createMethod, signatures, sootClass.getName());
        createMethod.setActiveBody(jimpleBody);
        processMain(psm, sootClass.getShortName(), sootClass.getName(), sootClass.getMethodByName("<init>").toString(), createMethod.toString());
    }

    /**
     * Handle the logic of aspect-oriented programming
     */
    private void aspectAnalysis(String configPath) {
        AOPParser aopParser = new AOPParser();
        Set<SootClass> allComponents = new HashSet<>(interfaceToBeans.values());
        List<AspectModel> allAspects = aopParser.getAllAspects(allComponents);
        List<AspectModel> allAdvices = new ArrayList<>();
        for (AspectModel aspect : allAspects) {

            HashMap<String, String> pcutmethod = new HashMap<>();
            for (SootMethod method : aspect.getSootClass().getMethods()) {
                VisibilityAnnotationTag annotationTags = (VisibilityAnnotationTag) method.getTag("VisibilityAnnotationTag");
                if (annotationTags != null && annotationTags.getAnnotations() != null) {
                    for (AnnotationTag annotation : annotationTags.getAnnotations()) {
                        if (annotation.getType().contains("Pointcut")) {
                            for (AnnotationElem elem : annotation.getElems()) {
                                AnnotationStringElem ase = (AnnotationStringElem) elem;
                                String expression = ase.getValue();
                                pcutmethod.put(method.getName(), expression);
                            }
                        }
                    }
                }
            }

            for (SootMethod aspectMethod : aspect.getSootClass().getMethods()) {
                VisibilityAnnotationTag annotationTags = (VisibilityAnnotationTag) aspectMethod.getTag("VisibilityAnnotationTag");
                if (annotationTags == null) {
                    continue;
                }
                for (AnnotationTag annotation : annotationTags.getAnnotations()) {
                    if (annotation.getType().contains("Lorg/aspectj/lang/annotation/")) {
                        if (annotation.getType().contains("Pointcut")||annotation.getType().contains("AfterThrowing")) break;
                        for (AnnotationElem elem : annotation.getElems()) {
                            AnnotationStringElem ase = (AnnotationStringElem) elem;
                            if (!ase.getName().equals("value") && !ase.getName().equals("pointcut")) {
                                continue;
                            }
                            String expression = ase.getValue();

                            for (String s : pcutmethod.keySet()) {
                                if (expression.contains(s)) {
                                    expression = expression.replace(s + "()", pcutmethod.get(s));
                                }
                            }
                            if ((expression.contains("execution") || expression.contains("within")
                                    || expression.contains("args")) || expression.contains("@annotation") || expression.contains("@within")) {
                                aopParser.processDiffAopExp(expression, aspectMethod);
                            }
                            if (EnumUtils.getEnumObject(annotation.getType()) != null) {
                                AspectModel adviceModel = getAspectModelInstance(aspect, expression, annotation, aspectMethod);
                                if (!allAdvices.contains(adviceModel)) {
                                    allAdvices.add(adviceModel);
                                }
                                Collections.sort(allAdvices);
                            }
                        }
                    }
                }
            }
        }

        allAdvices.addAll(xmlaspectAnalysis(configPath, aopParser));
        aopParser.addAdviceToTarget(allAdvices);
        for (AOPTargetModel aopTargetModel : AOPParser.modelMap.values()) {
            aopAnalysis.processWeave(aopTargetModel);
        }
    }

    private XMLDocumentHolder getXMLHolder(Set<String> xmlpaths) {
        if (xmlpaths.size() == 0) {
            return null;
        }
        XMLDocumentHolder holder = new XMLDocumentHolder();
        for (String xmlpath : xmlpaths) {
            Document document = holder.getDocument(xmlpath);
            if (document != null) {
                holder.addElements(document);
                holder.hasArgConstructorBean(document);
            }
        }
        return holder;
    }

    /**
     * Get the collection of sootclass objects of the bean configured in the xml file
     *
     * @param xmlpaths  The path of the xml configuration file, it can be multiple,  pass in a set collection to store multiple xml paths
     * @return  Return the sootClass of the specific implementation class of the bean configured in these xml files
     */
    public Set<XmlBeanClazz> getXMLBeanSootClazzes(Set<String> xmlpaths) {
        XMLDocumentHolder holder = getXMLHolder(xmlpaths);
        if (holder == null) {
            return null;
        }
        Map<String, String> allClassMap = holder.getAllClassMap();
        Set<XmlBeanClazz> res = new HashSet<>();
        for (String value : allClassMap.values()) {
            String[] split = value.split(";");
            SootClass xmlsootclass = Scene.v().getSootClass(split[0]);
            XmlBeanClazz xmlBeanClazz = new XmlBeanClazz(xmlsootclass, split[1]);
            res.add(xmlBeanClazz);
        }
        return res;
    }

    static class XmlBeanClazz {
        private SootClass sootClass;
        private String scope;

        public XmlBeanClazz() {
        }

        public XmlBeanClazz(SootClass sootClass, String scope) {
            this.sootClass = sootClass;
            this.scope = scope;
        }

        @Override
        public String toString() {
            return "XmlBeanClazz{" +
                    "sootClass=" + sootClass +
                    ", scope='" + scope + '\'' +
                    '}';
        }

        public SootClass getSootClass() {
            return sootClass;
        }

        public void setSootClass(SootClass sootClass) {
            this.sootClass = sootClass;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    /**
     * Parse out the aop configuration information from the xml file and encapsulate it into the AopXMLResultBean object
     * Then judge whether these configured aspect methods are included in the aspect class,
     * if exist, call the processDiffAopExp() method to process the aspect expression.
     *
     * @return
     */
    private List<AspectModel> xmlaspectAnalysis(String configPath, AOPParser aopParser) {
        XMLDocumentHolder holder = new XMLDocumentHolder();
        List<AspectModel> allAdvices = new ArrayList<>();
        Set<String> bean_xml_paths = FileUtils.getBeanXmlPaths(configPath, "bean_xml_paths");
        if (bean_xml_paths.size() == 0) {
            return allAdvices;
        }
        for (String bean_xml_path : bean_xml_paths) {
            Document document = holder.getDocument(bean_xml_path);
            if (document == null) {
                continue;
            }
            holder.addElements(document);
            List<AopXMLResultBean> beanList = holder.processAopElements(document);
            Set<String> aopClasses = new HashSet<>();
            for (AopXMLResultBean aopXMLResultBean : beanList) {
                aopClasses.add(aopXMLResultBean.getAopclass());
            }
            for (String aopclass : aopClasses) {
                SootClass sootClass = Scene.v().getSootClass(aopclass);
                for (SootMethod method : sootClass.getMethods()) {
                    for (AopXMLResultBean aopXMLResultBean : beanList) {
                        if (method.getName().equals(aopXMLResultBean.getAopmethod())) {
                            aopParser.processDiffAopExp(aopXMLResultBean.getExper(), method);
                            AspectModel aspectModel = copyXmlBeanToAspectModel(aopXMLResultBean, sootClass, method);
                            allAdvices.add(aspectModel);
                            Collections.sort(allAdvices);
                        }
                    }
                }
            }
        }
        return allAdvices;
    }

    /**
     * Get an instance of the advice method
     *
     * @param aspect       Advice class that needs to be constructed
     * @param expression   pointcut expression
     * @param annotation   What kind of advice method is the pointcut expression
     * @param aspectMethod  The specific method of advice
     * @return  Thr instance of the advice method
     */
    private AspectModel getAspectModelInstance(AspectModel aspect, String expression, AnnotationTag annotation, SootMethod aspectMethod) {
        AspectModel adviceModel;
        if (aspectModelMap.containsKey(aspectMethod.toString())) {
            adviceModel = aspectModelMap.get(aspectMethod.toString());
            adviceModel.addPointcutExpressions(expression);
        } else {
            adviceModel = new AspectModel();
            adviceModel.setOrder(aspect.getOrder());
            adviceModel.setSootClass(aspect.getSootClass());
            adviceModel.addPointcutExpressions(expression);
            adviceModel.setSootMethod(aspectMethod);
            adviceModel.setAnnotation(EnumUtils.getEnumObject(annotation.getType()));
            aspectModelMap.put(aspectMethod.toString(), adviceModel);
        }
        return adviceModel;
    }

    private AspectModel copyXmlBeanToAspectModel(AopXMLResultBean aopXMLResultBean, SootClass sootClass, SootMethod sootMethod) {
        AspectModel adviceModel;
        if (aspectModelMap.containsKey(sootMethod.toString())) {
            adviceModel = aspectModelMap.get(sootMethod.toString());
            adviceModel.addPointcutExpressions(aopXMLResultBean.getExper());
        } else {
            adviceModel = new AspectModel();
            adviceModel.setOrder(aopXMLResultBean.getOrder());
            adviceModel.setSootClass(sootClass);
            adviceModel.addPointcutExpressions(aopXMLResultBean.getExper());
            adviceModel.setSootMethod(sootMethod);
            List<AdviceEnum> collect = Arrays.stream(AdviceEnum.values()).filter(x -> x.getAnnotationClassName().toLowerCase().contains(aopXMLResultBean.getActivetype())).collect(Collectors.toList());
            adviceModel.setAnnotation(collect != null ? collect.get(0) : AdviceEnum.AOP_BEFORE);
            aspectModelMap.put(sootMethod.toString(), adviceModel);
        }
        return adviceModel;
    }

    /**
     * In the main method of the main startup class Application, add an active call to testRun statement.
     * TestRun is a method of actively calling sink points added by each tested class.
     *
     * @param method   The main method of the main startup class Application
     * @param objName  Variable name of the constructed object
     * @param objType  The full class name of the constructed object type
     * @param initSign  The signature of the init method of the constructed object is used to instantiate the object
     * @param runSign  testRun2  The signature of the method
     */
    public void processMain(SootMethod method, String objName, String objType, String initSign, String runSign) {
        JimpleBody body = (JimpleBody) method.retrieveActiveBody();
        Local localModel = jimpleUtils.addLocalVar(objName, objType, body);

        PatchingChain<Unit> units = body.getUnits();
        units.removeIf(unit -> unit instanceof JReturnVoidStmt);
        jimpleUtils.createAssignStmt(localModel, objType, units);
        units.add(jimpleUtils.specialCallStatement(localModel, initSign));
        units.add(jimpleUtils.virtualCallStatement(localModel, runSign));
        jimpleUtils.addVoidReturnStmt(units);
    }
}
