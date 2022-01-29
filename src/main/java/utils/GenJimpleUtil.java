package utils;

import soot.Printer;
import soot.SootClass;
import soot.SourceLocator;
import soot.options.Options;

import java.io.*;

/**
 * @ClassName   GenJimpleUtil
 * @Description new a file of Jimple
 */
public class GenJimpleUtil {
    public static void write(SootClass sClass) {
        OutputStream streamOut = null;
        try {
            String filename = SourceLocator.v().getFileNameFor(sClass, Options.output_format_shimple);
            String processProjectName = "PointerAnalysis";
            filename = "ShimpleResult" + File.separator + processProjectName + File.separator + filename;
            int i = filename.lastIndexOf(File.separator);
            String f1 = filename.substring(0, i);
            File file1 = new File(f1);
            if (!file1.exists()) {
                file1.mkdirs();
            }
            File file2 = new File(filename);
            if (!file2.exists()) {
                file2.createNewFile();
            }
            File file3 = new File("sootOutput");
            if (file3.exists()) {
                file3.delete();
            }
            streamOut = new FileOutputStream(filename);
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            Printer.v().printTo(sClass, writerOut);
            writerOut.flush();
            writerOut.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (streamOut != null)
                try {
                    streamOut.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
        }
    }
}
