package utils;

import enums.AdviceEnum;

/**
 * @ClassName   EnumUtils
 * @Description Enumeration of annotations
 */
public class EnumUtils {
    public static AdviceEnum getEnumObject(Object value) {
        for (AdviceEnum adviceEnum : AdviceEnum.values()) {
            if (adviceEnum.getAnnotationClassName().equals(value)) {
                return adviceEnum;
            }
        }
        return null;
    }
}
