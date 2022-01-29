# Jasmine
Jasmine is a static analysis framework for Spring applications.
## using Jasmine in Soot or Doop
### Soot
Jasmine provides examples of collaboration with the Soot framework.

* First, compile the target project into Jar and decompress it. Put the decompressed folder in the *./demo* directory.

* Then, modify the *bean_xml_paths* property in the *./src/main/resources/config.properties* file to the absolute path address of the Spring-special application XML file of the target project.

* After that, change the project name in the *sourceDirectory* and *dependencyDirectory* fields in the class *ParserSpringMain* to the target project, and then we can run it. In the *initializeSoot* method, SPARK mode (default) or CHA mode can be selected.

The class *analysis.ForwardTransformer* output the application reachable methods and call edges of the target project.

### Doop
First, pull doop from https://bitbucket.org/yanniss/doop/src/master/, and add dependencies in the *./doop/generators/soot-fact-generator/build.gradle* file.

```groovy
compile group: 'com.alibaba', name: 'fastjson', version: '1.2.73'
compile group: 'org.aspectj', name: 'aspectjweaver', version: '1.9.6'
runtime group: 'org.aspectj', name: 'aspectjweaver', version: '1.9.6'
compile group: 'org.dom4j', name: 'dom4j', version: '2.0.3'
```

* Then, use gradle to build Jasmine and import it into the Doop project as a dependency, and add the following two lines of code at line 226 in the *invokeSoot* method of the *org.clyze.doop.soot.Main*.

```Java
CreateEdge createEdge = new CreateEdge();
createEdge.initCallGraph("/src/main/resources/config.properties"); //Replace according to actual
```

* After that，running Doop.
If you only want to run Jasmine, add the *./src/main/resources/rules-jasmine.dl* file into *./doop/souffle-logic/addons/open-programs* directory.

```
$  ./doop -i ../doop-benchmarks/javaee-benchmarks/WebGoat.war -a context-insensitive --open-programs jasmine
```
