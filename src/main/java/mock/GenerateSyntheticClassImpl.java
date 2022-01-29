package mock;

import analysis.CreateEdge;
import bean.ConstructorArgBean;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JIdentityStmt;
import soot.util.Chain;
import utils.JimpleUtils;

import java.util.*;

/**
 * @ClassName GenerateSyntheticClassImpl
 * @Description Generate synthetic classes for all interfaces or abstract classes for
 * which no concrete implementation class can be found
 **/
public class GenerateSyntheticClassImpl implements GenerateSyntheticClass {

    private final JimpleUtils jimpleUtils = new JimpleUtils();
    private static final Map<String, SootClass> syntheticMethodImpls = new HashMap<>();
    private Map<String, Set<String>> classMethodsMap = new HashMap<>();

    /**
     * Generate a custom JoinPoint implementation class
     *
     * @param abstractClass An abstract class that requires a concrete implementation class
     * @return Implementation class of the generated JoinPoint
     */
    @Override
    public SootClass generateJoinPointImpl(SootClass abstractClass) {
        SootClass customImplClass;
        String implClassName = "synthetic.method." + abstractClass.getShortName() + "Impl";
        if (syntheticMethodImpls.containsKey(implClassName)) {
            customImplClass = syntheticMethodImpls.get(implClassName);
        } else {
            customImplClass = createSubClass(implClassName, abstractClass, Scene.v().getSootClass("java.lang.Object"));
            customImplClass.addInterface(Scene.v().getSootClass("org.aspectj.lang.JoinPoint"));
            Scene.v().addClass(customImplClass);
            // If this line is not added, it will cause spark analysis to ignore the corresponding custom class
            customImplClass.setApplicationClass();
            SootField field = new SootField("args", ArrayType.v(RefType.v("java.lang.Object"), 1));
            customImplClass.addField(field);
            SootMethod initMethod = jimpleUtils.genDefaultConstructor(customImplClass);
            customImplClass.addMethod(initMethod);

            for (SootClass anInterface : customImplClass.getInterfaces()) {
                implCommonMethod(customImplClass, anInterface);
            }
            for (SootClass abstractClassInterface : abstractClass.getInterfaces()) {
                implCommonMethod(customImplClass, abstractClassInterface);
            }
            customImplClass.addMethod(jimpleUtils.genCustomMethod(customImplClass,
                    "setArgs_synthetic",
                    Arrays.asList(new Type[]{ArrayType.v(RefType.v("java.lang.Object"), 1)}),
                    VoidType.v()));
            syntheticMethodImpls.put(implClassName, customImplClass);
        }
        return customImplClass;
    }

    /**
     * Generate a custom Mapper implementation class
     *
     * @param interfaceClass Need to concretely implement the interface of the class
     * @return Implementation class of the generated Mapper
     */
    @Override
    public SootClass generateMapperImpl(SootClass interfaceClass) {
        SootClass mapperImplClass;
        String implClassName = "synthetic.method." + interfaceClass.getShortName() + "Impl";
        if (syntheticMethodImpls.containsKey(implClassName)) {
            mapperImplClass = syntheticMethodImpls.get(implClassName);
        } else {
            mapperImplClass = createSubClass(implClassName, interfaceClass, Scene.v().getSootClass("java.lang.Object"));
            Scene.v().addClass(mapperImplClass);
            mapperImplClass.setApplicationClass();
            SootMethod initMethod = jimpleUtils.genDefaultConstructor(mapperImplClass);
            mapperImplClass.addMethod(initMethod);
            implCommonMethod(mapperImplClass, interfaceClass);
            syntheticMethodImpls.put(implClassName, mapperImplClass);
        }
        return mapperImplClass;
    }

    /**
     * Simulate Spring AOP mechanism to generate proxy for each target class
     *
     * @param targetSootClass The target class or interface that needs to generate the proxy
     * @return proxy class
     */
    @Override
    public SootClass generateProxy(SootClass targetSootClass) {
        SootClass proxyClass;
        String proxyClassName = targetSootClass.getName() + "$$SpringCGLIB";// + targetSootClass.hashCode();
        if (syntheticMethodImpls.containsKey(proxyClassName)) {
            proxyClass = syntheticMethodImpls.get(proxyClassName);
        } else {
            boolean isInterface = targetSootClass.isInterface();
            if (isInterface) {
                proxyClass = createSubClass(proxyClassName, targetSootClass, Scene.v().getSootClass("java.lang.Object"));
            } else {
                proxyClass = createSubClass(proxyClassName, null, targetSootClass);
            }
            Scene.v().addClass(proxyClass);
            SootField field = new SootField("target", targetSootClass.getType());
            proxyClass.addField(field);
            proxyClass.setApplicationClass();
            SootMethod initMethod =null;
            if(CreateEdge.prototypeComponents.contains(proxyClass)){
                initMethod = jimpleUtils.genDefaultConstructor(proxyClass, field, false);
            }else {
                initMethod = jimpleUtils.genDefaultConstructor(proxyClass, field, true);
            }

            proxyClass.addMethod(initMethod);
            if (isInterface) {
                implCommonMethod(proxyClass, targetSootClass);
            } else {
                extendCommonMethod(proxyClass, targetSootClass);
            }
            syntheticMethodImpls.put(proxyClassName, proxyClass);
        }
        return proxyClass;
    }

    /**
     * Simulate a singleton factory in Spring
     *
     * @param beans the bean that need to generate with singleton
     */
    @Override
    public void generateSingletonBeanFactory(Set<SootClass> beans,Map<String, List<ConstructorArgBean>> collect) {
        if(collect !=null && collect.size() > 0){
            for (String s : collect.keySet()) {
                beans.add(Scene.v().getSootClass(s));
            }
        }
        SootClass singletonFactory;
        String singletonFactoryName = "synthetic.method.SingletonFactory";
        singletonFactory = createSubClass(singletonFactoryName, null, Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(singletonFactory);
        singletonFactory.setApplicationClass();
        Set<SootField> fields = new HashSet<>();
        for (SootClass bean : beans) {
            SootField field = new SootField(bean.getShortName(), bean.getType(), Modifier.PUBLIC + Modifier.STATIC);
            fields.add(field);
            singletonFactory.addField(field);
            singletonFactory.addMethod(jimpleUtils.genStaticCustomMethod(singletonFactory,
                    "get" + bean.getShortName(),
                    null,
                    bean.getType(), field));
        }
        SootMethod initMethod = jimpleUtils.genDefaultConstructor(singletonFactory, null, false);
        singletonFactory.addMethod(initMethod);

        SootMethod clinitMethod = jimpleUtils.genDefaultClinit(singletonFactory, fields,collect);
        singletonFactory.addMethod(clinitMethod);
    }

    /**
     * Simulate the implementation of HttpServlet
     * @param abstractClass The target class or interface that needs to generate the proxy
     * @return HttpServlet implementation class
     */
    @Override
    public SootClass generateHttpServlet(SootClass abstractClass) {
        SootClass customImplClass;
        String implClassName = "synthetic.method." + abstractClass.getShortName() + "Impl";
        if (syntheticMethodImpls.containsKey(implClassName)) {
            customImplClass = syntheticMethodImpls.get(implClassName);
        } else {
            customImplClass = createSubClass(implClassName, abstractClass, Scene.v().getSootClass("java.lang.Object"));
            Scene.v().addClass(customImplClass);
            customImplClass.setApplicationClass();
            SootMethod initMethod = jimpleUtils.genDefaultConstructor(customImplClass);
            customImplClass.addMethod(initMethod);
            implCommonMethod(customImplClass, abstractClass);
            syntheticMethodImpls.put(implClassName, customImplClass);
        }
        return customImplClass;
    }

    /**
     * Initialize a custom class
     *
     * @param implClassName  class name
     * @param interfaceClass the interface that need to implement
     * @return custom class
     */
    public SootClass createSubClass(String implClassName, SootClass interfaceClass, SootClass superClass) {
        SootClass customImplClass = new SootClass(implClassName);
        customImplClass.setResolvingLevel(SootClass.BODIES);
        if (interfaceClass != null) {
            customImplClass.addInterface(interfaceClass);
        }
        customImplClass.setModifiers(Modifier.PUBLIC);
        customImplClass.setSuperclass(superClass);
        return customImplClass;
    }

    /**
     * Implement interface methods in custom classes
     *
     * @param customImplClass custom class
     * @param interfaceClass  the interface that need to implement
     */
    public void implCommonMethod(SootClass customImplClass, SootClass interfaceClass) {
        for (SootMethod method : interfaceClass.getMethods()) {
            try {
                customImplClass.addMethod(jimpleUtils.genCustomMethod(customImplClass,
                        method.getName(),
                        method.getParameterTypes(),
                        method.getReturnType()));
            } catch (RuntimeException ignored) {}
        }
        for (SootClass superInterface : interfaceClass.getInterfaces()) {
            implCommonMethod(customImplClass, superInterface);
        }
    }

    /**
     * Method overridden in a custom class
     *
     * @param customSubClass custom class
     * @param superClass     Concrete classes that need to be inherited
     */
    public void extendCommonMethod(SootClass customSubClass, SootClass superClass) {
        for (SootMethod superMethod : superClass.getMethods()) {
            if (superMethod.isStatic() || superMethod.isPrivate()
                    || superMethod.isFinal() || superMethod.getName().contains("<init>")
                    || superMethod.getName().contains("<clinit>")) {
                continue;
            }
            SootMethod subMethod = new SootMethod(superMethod.getName(),
                    superMethod.getParameterTypes(),
                    superMethod.getReturnType(),
                    superMethod.getModifiers());
            customSubClass.addMethod(subMethod);
            JimpleBody subMethodBody = (JimpleBody) superMethod.retrieveActiveBody().clone();
            Chain<Local> locals = subMethodBody.getLocals();
            PatchingChain<Unit> units = subMethodBody.getUnits();
            units.removeIf(unit -> !(unit instanceof JIdentityStmt && unit.toString().contains("@parameter")));
            subMethodBody.getTraps().clear();
            Set<Local> tmplocal = new HashSet<>();
            if (units.size() == 0) {
                locals.removeIf(Objects::nonNull);
            } else {
                for (Unit unit : units) {
                    JIdentityStmt jstmt = (JIdentityStmt) unit;
                    if(locals.contains(jstmt.getLeftOpBox().getValue())){
                        tmplocal.add((Local) jstmt.getLeftOpBox().getValue());
                    }
                }
                locals.removeIf(local -> !tmplocal.contains(local));
            }
            Local thisRef = jimpleUtils.addLocalVar("this", customSubClass.getType(), subMethodBody);
            jimpleUtils.createIdentityStmt(thisRef, jimpleUtils.createThisRef(customSubClass.getType()), units);
            Local returnRef = null;
            if (!(subMethod.getReturnType() instanceof VoidType)) {
                returnRef = jimpleUtils.addLocalVar("returnRef", subMethod.getReturnType(), subMethodBody);
            }
            for (SootField field : customSubClass.getFields()) {
                Local tmpRef = jimpleUtils.addLocalVar("localTarget", field.getType(), subMethodBody);
                jimpleUtils.createAssignStmt(tmpRef, jimpleUtils.createInstanceFieldRef(thisRef, field.makeRef()), units);
                if (!(superMethod.getReturnType() instanceof VoidType)) {
                    Value returnValue = jimpleUtils.createVirtualInvokeExpr(tmpRef, superMethod, subMethodBody.getParameterLocals());
                    jimpleUtils.createAssignStmt(returnRef, returnValue, units);
                } else {
                    units.add(jimpleUtils.virtualCallStatement(tmpRef, superMethod.toString(), subMethodBody.getParameterLocals()));
                }
            }

            if (returnRef != null) {
                jimpleUtils.addCommonReturnStmt(returnRef, units);
            } else {
                jimpleUtils.addVoidReturnStmt(units);
            }
            subMethod.setActiveBody(subMethodBody);
        }
    }
}
