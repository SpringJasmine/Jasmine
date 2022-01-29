package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @ClassName   FileUtils
 * @Description reading files
 */
public class FileUtils {

    public static void writeFile(String filePath, String filename, String sets) {
        FileWriter fw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(filePath + filename, true);
            out = new PrintWriter(fw);
            out.write(sets);
            out.println();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert fw != null;
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert out != null;
            out.close();
        }
    }

    public static Set<String> getBeanXmlPaths(String configPath, String keyname) {
        Set<String> res = new HashSet<>();
        if(configPath == null || "".equals(configPath)){
            return res;
        }
        String property = ConfigUtil.getProperties(configPath).getProperty(keyname);
        if (property != null) {
            String[] split = property.split(",");
            Collections.addAll(res, split);
        }
        return res;
    }

    public static String getApplicationMain(String configPath, String keyname) {
        return ConfigUtil.getProperties(configPath).getProperty(keyname);
    }

}
