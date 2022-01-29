package mock;

import soot.*;
import soot.jimple.JimpleBody;

/**
 * @ClassName MockObject
 * @Description Simulate the initialization behavior of the class
 **/
public interface MockObject {
    /**
     * Simulate the behavior of JoinPoint in the program
     *
     * @param body  the method body which contains to JoinPoint initialized
     * @param units the method body which contains to JoinPoint initialized
     */
    void mockJoinPoint(JimpleBody body, PatchingChain<Unit> units);

    /**
     * Simulate the initialization of the entity class in the program
     *
     * @param body      the method body which contains to entity class initialized
     * @param units     the method body which contains to entity class initialized
     * @param sootClass controller class
     * @param toCall    controller method
     * @return Local variables of entity classes
     */
    Local mockBean(JimpleBody body, PatchingChain<Unit> units, SootClass sootClass, SootMethod toCall);

    Local mockHttpServlet(JimpleBody body, PatchingChain<Unit> units, SootClass sootClass, SootMethod toCall);
}
