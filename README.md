# The code, analysis scripts and results for ASE 2022 Artifact Evaluation

Version: 1.1
Paper: Jasmine: A Static Analysis Framework for Spring Core Technologies (#175)

This document is to help users reproduce the results we reported in our submission. It contains the following descriptions

## 0. Artifact Expectation

The code and scripts for the tools we build are published in this repository as well as other repositories for SpringJasmine users. **All the experiments were carried out on a Docker environment which deployed on an Intel(R) Xeon(R) CPU E5-2650 v4 @ 2.20GHz (4 x 12 core) and 128GB of RAM.** Both can be executed using Docker 20.10.8. We hope that users can reproduce our experiments using this version or a later version of Docker. Doop framework related experiments are run on Docker 19.03.12, Soot and FlowDroid related experiments are run on IntelliJ Idea.

## 1. Environment Setup

Pull Jasmine from GitHub.

```sh
$ git clone https://github.com/SpringJasmine/Doop_Jasmine.git
```

### 1.1. Docker

Execute the Dockerfile in the [Jasmine repository](https://github.com/SpringJasmine/Jasmine) to generate a docker container, ```/localdata``` is the folder path of your machine. Please change it according to the actual situation.

```sh
$ cd Jasmine
$ docker build -t jasmine:v1 .
$ docker run -it -h Jasmine.gt --privileged=True -e "container=docker" -v /localdata:/data --net=host --name="Jasmine" jasmine:v1 bash
# /localdata is your local folder path
```

### 1.2 IntelliJ IDEA

Some experiment uses **maven and IntelliJ IDEA**, the maven version is 3.6.1, and the Java version is Java 8.

## 2. Tab Section 5.2

This table mainly shows the improvement of Jasmine's detection ability of JackEE, including TABLE II and Fig.4.

### 2.1 Doop version comparison

#### 2.1.1 JackEE, Default and Jasmine use context-insensitive analysis (corresponding to ID-1-EntryPoint, ID-4-SpringAOP, ID-7-Reachable  Methods, ID-8-Application Edges)

* Run the following command.

```sh
# JackEE
$ bash /root/JackEEforSpringDemo.bash

# Defalut
$ bash /root/DefaultforSpringDemo.bash

# Jasmine
$ bash /root/JasmineforSpringDemo.bash
```

#### 2.1.2 JackEE, Default and Jasmine use context-insensitive analysis and taint analysis (corresponding to ID-2-DI(Singleton), ID-3-DI(Prototype), ID-5-InfoLeak)

* Run the following command.

```sh
# JackEE
$ bash /root/JackEEforInfoSpringDemo.bash

# Defalut
$ bash /root/DefaultforInfoSpringDemo.bash

# Jasmine
$ bash /root/JasmineforInfoSpringDemo.bash
```

### 2.2 Soot version comparison

 This project mainly uses maven and IntelliJ IDEA, the maven version is 3.6.1, and the Java version is Java 8.

#### 2.2.1 CHA, SPARK and Jasmine（corresponding to ID-1-EntryPoint, ID-4-SpringAOP, ID-7-Reachable  Methods, ID-8-Application Edges）

* Open the project with IntelliJ IDEA.

* Modify ```src/main/resources/config.properties```

```properties
# Modify bean_xml_paths to the absolute path where the corresponding file
bean_xml_paths = /User/Jasmine/demo/target-demo-0.0.1/BOOT-INF/classes/bean.xml
```

* Modify```ParserSpringMain```

```java
//When the following three lines are not commented, jasmine is turned on
29: CreateEdge createEdge = new CreateEdge();
30: String path = "config.properties"; // Modify field to the path of config.properties in resources
31: createEdge.initCallGraph(path);
...    
74: // turn on SPARK and Jasmine
    Options.v().setPhaseOption("cg.spark", "on");
    Options.v().setPhaseOption("cg.spark", "verbose:true");
    Options.v().setPhaseOption("cg.spark", "enabled:true");
    Options.v().setPhaseOption("cg.spark", "propagator:worklist");
    Options.v().setPhaseOption("cg.spark", "simple-edges-bidirectional:false");
    Options.v().setPhaseOption("cg.spark", "on-fly-cg:true");
    // Options.v().setPhaseOption("cg.spark", "pre-jimplify:true");
    Options.v().setPhaseOption("cg.spark", "double-set-old:hybrid");
    Options.v().setPhaseOption("cg.spark", "double-set-new:hybrid");
    Options.v().setPhaseOption("cg.spark", "set-impl:double");
    Options.v().setPhaseOption("cg.spark", "apponly:true");
    Options.v().setPhaseOption("cg.spark", "simple-edges-bidirectional:false");
87: Options.v().set_verbose(true);
88:
89: // turn on CHA
    // Options.v().setPhaseOption("cg.cha", "on");
    // Options.v().setPhaseOption("cg.cha", "enabled:true");
    // Options.v().setPhaseOption("cg.cha", "verbose:true");
    // Options.v().setPhaseOption("cg.cha", "apponly:true");
94: // Options.v().set_verbose(true);
```

* Execute the main method in ```ParserSpringMain```

#### 2.2.2 SPARK, Jasmine context-sensitive（corresponding to ID-2-DI(Singleton), ID-3-DI(Prototype)）

* Download TURNER

```sh
$ wget http://www.cse.unsw.edu.au/~corg/turner/Turner-src-v1.0.tar.xz
```

* Open the project with IntelliJ IDEA.
* Create a libs folder under the project directory and add **PointerAnalysis-1.0-SNAPSHOT.jar **to libs.
* Add add **PointerAnalysis-1.0-SNAPSHOT.jar** in the dist directory as a project dependency

* Add Jasmine in ```driver.Main```

```java
24: public static PTA run(String[] args) {
25:     PTA pta;
26:     new PTAOption().parseCommandLine(args);
27:     setupSoot();
        // Modify field to the path of config.properties in section 1.2.1
        CreateEdge createEdge = new CreateEdge(); //add
        createEdge.initCallGraph("config.properties"); // add
```

* Execute the main method in ```driver.Main```

## 3.Tab Section 5.3

This tab shows the improvement of Jasmine's detection ability of JackEE, including TABLE III, TABLE IV, Fig.5, and Fig.6. Due to differences in the performance of the relevant machines, there may be some minor differences in the correlation results.

### 3.2 Run Jasmine and JackEE

The result of **TABLE III, Fig.5 and Fig.6** can be counted by executing the following commands.

```sh
$ cd /root/newdoop
# Jas_JackEE
$ bash /root/TurnOnJasmine.bash
$ python3 /root/newdoop/runjasmine.py
# JackEE
$ bash /root/TurnOffJasmine.bash
$ python3 /root/newdoop/runjackee.py
```

### 3.3 Run data processing scripts

Dockerfile generates ```config.properties``` in ```/root```. Please modify items according to the output items under``` /data/doop/out/```.

```properties
# config.properties
process_project_name = mall-admin
Jasmine_CallGraph_Path = /data/doop/out/jasminemall-admin/database/CallGraphEdge.csv
JackEE_CallGraph_Path = /data/doop/out/jackeemall-admin/database/CallGraphEdge.csv
Jasmine_ReachableMethod_Path = /data/doop/out/jasminemall-admin/database/Stats_Simple_Application_ReachableMethod.csv
JackEE_ReachableMethod_Path = /data/doop/out/jackeemall-admin/database/Stats_Simple_Application_ReachableMethod.csv
resultPath = /root/output/
```
![image-20220830160118097](https://github.com/SpringJasmine/IMAGE/blob/main/image-20220830160118097.png)

The results of **TABLE III and TABLE IV** can be counted by executing the following commands.

```sh
$ java -jar /root/DataCollect-1.0-SNAPSHOT.jar -c /root/config.properties
```
![image-20220830155609790](https://github.com/SpringJasmine/IMAGE/blob/main/image-20220830155609790.png)

## 4. Tab Section 5.4

This table mainly shows the improvement of Jasmine's detection ability of FlowDroid, including the acquisition of all data and charts in Section 5.4 of the paper, mainly including TABLE V. This project mainly uses maven and IntelliJ IDEA, the maven version is 3.6.1, and the Java version is Java 8.

### 4.1 Environment

* The first step is to pull the FlowDroid_Jasmine project and the benchmark to the local.

```sh
$ git clone https://github.com/SpringJasmine/FlowDroid_Jasmine.git
$ git clone https://github.com/SpringJasmine/Benchmark_FlowDroid
$ cd Benchmark_FlowDroid
$ pwd # Copy the absolute path of Benchmark_FlowDroid
```

* The second step is to open the project with IntelliJ IDEA.

* The third step is to modify the relevant configuration under the **dataleak** Module

```java
// com.taint.analysis.utils.BenchmarksConfig
public class BenchmarksConfig {
    // Modify "/users/flowdroidplus/demo" to the absolute path of Benchmark_Flowdroid
    private static String basePath = "/Users/FlowDroidPlus/demo";
    ...
```

```java
// com.taint.analysis.Main
// Modify the field of benchmark to project name and the field of analysisalgorithm to the detection algorithm (cha, spark, Jasmine)
13:    public static String benchmark = "mall-admin";
14:    public static String analysisAlgorithm = "cha";
```

* The fourth step is to add PointerAnalysis-1.0-SNAPSHOT.jar in the dist directory as a project dependency

* The fifth step is to modify the content of ```../FlowDroidPlus/dataleak/src/main/resources/config.json``` according to the actual environment

```json
// Modify "source" and "edge_config" to the path of the corresponding file
{
  "source": "/Users/FlowDroidPlus/dataleak/src/main/resources/source.json",
  "main_class": "com.ruoyi.RuoYiApplication",
  "edge_config": "/Users/FlowDroidPlus/dataleak/src/main/resources/config.properties"
}
```

### 4.2 run FlowDroid

* First, modify the relevant configuration under the **soot-infoflow** Module

```java
// soot.jimple.infoflow.Infoflow
// Modify the value of analysisAlgorithm according to step 4 (cha, spark, jasmine)
269:            String analysisAlgorithm = "cha";
270:            if (analysisAlgorithm.equals("cha")) {
271:                config.setCallgraphAlgorithm(CallgraphAlgorithm.CHA);
272:            } else {
273:                config.setCallgraphAlgorithm(CallgraphAlgorithm.SPARK);
274:            }
```

* Execute the main method in ```com.taint.analysis.Main``` under the **dataleak** Module
