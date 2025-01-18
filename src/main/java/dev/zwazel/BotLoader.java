package dev.zwazel;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class BotLoader {

    public static BotInterface loadBot(String pathToJavaFile) throws Exception {
        // 1. Compile .java file in memory or to a temp folder
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // Use a StandardJavaFileManager to compile the file
        File file = new File(pathToJavaFile);
        Iterable<? extends JavaFileObject> compilationUnits =
            compiler.getStandardFileManager(null, null, null)
                    .getJavaFileObjectsFromFiles(List.of(file));

        compiler.getTask(null, null, null, null, null, compilationUnits).call();

        // 2. Load the compiled class
        //    This assumes your .class is output in the same directory as the .java
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] {
            new File(file.getParent()).toURI().toURL()
        });

        // 3. The class name should match the .java filename
        //    or we parse the file to find the declared class name
        String className = getClassNameFromFile(file);

        Class<?> botClass = Class.forName(className, true, classLoader);

        // 4. Instantiate and cast
        Object botInstance = botClass.getDeclaredConstructor().newInstance();
        return (BotInterface) botInstance;
    }

    private static String getClassNameFromFile(File file) {
        // For simplicity, assume the file name is the class name w/o .java
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }
}
