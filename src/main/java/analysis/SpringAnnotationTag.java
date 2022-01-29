package analysis;

/**
 * @ClassName SpringAnnotationTag
 **/
public class SpringAnnotationTag {
    public static final int BEAN = 1;
    public static final int PROTOTYPE = 2;
    public static final int MAPPER = 4;

    public SpringAnnotationTag() {
    }

    public static boolean isBean(int m) {
        return (m & 1) != 0;
    }

    public static boolean isPrototype(int m) {
        return (m & 2) != 0;
    }

    public static boolean isMapper(int m) {
        return (m & 4) != 0;
    }


}
