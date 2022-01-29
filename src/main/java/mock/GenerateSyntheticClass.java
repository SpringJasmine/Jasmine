package mock;

import bean.ConstructorArgBean;
import soot.SootClass;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName GenerateSyntheticClass
 * @Description Generate synthetic classes for all interfaces or abstract classes for
 * which no concrete implementation class can be found
 **/
public interface GenerateSyntheticClass {
    /**
     * Generate a custom JoinPoint implementation class
     *
     * @param abstractClass An abstract class that requires a concrete implementation class
     * @return Implementation class of the generated JoinPoint
     */
    SootClass generateJoinPointImpl(SootClass abstractClass);

    /**
     * Generate a custom Mapper implementation class
     *
     * @param interfaceClass Need to concretely implement the interface of the class
     * @return Implementation class of the generated Mapper
     */
    SootClass generateMapperImpl(SootClass interfaceClass);

    /**
     * Simulate Spring AOP mechanism to generate proxy for each target class
     *
     * @param sootClass The target class or interface that needs to generate the proxy
     * @return proxy class
     */
    SootClass generateProxy(SootClass sootClass);

    /**
     * Simulate a singleton factory in Spring
     *
     * @param beans the bean that need to generate with singleton
     */
    void generateSingletonBeanFactory(Set<SootClass> beans, Map<String, List<ConstructorArgBean>> collect);

    /**
     * Simulate the implementation of HttpServlet
     * @param abstractClass The target class or interface that needs to generate the proxy
     * @return HttpServlet implementation class
     */
    SootClass generateHttpServlet(SootClass abstractClass);
}
