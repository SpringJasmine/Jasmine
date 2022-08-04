# The code, analysis scripts and results for ASE 2022 Artifact Evaluation

Version: 1.0
Paper: Jasmine: A Static Analysis Framework for Spring Core Technologies (#175)

This document is to help users reproduce the results we reported in our submission. It contains the following descriptions

## 0. Artifact Expectation

The code and scripts for the tools we build are published in this repository as well as other repositories for SpringJasmine users. The experiments are run on AMD machines, please do not use ARM architecture servers. Both can be executed using Docker 20.10.8. We hope that users can reproduce our experiments using this version or a later version of Docker. Doop framework related experiments are run on Docker 19.03.12, Soot and FlowDroid related experiments are run on IntelliJ Idea.

## 1. Tab TABLE II

This table mainly shows the improvement of Jasmine's detection ability of JackEE, including the acquisition of all data and charts in Section 5.2 of the paper, mainly including TABLE II and Fig.4.

### 1.1 Doop version comparison

Please refer to **2. TABLE III** for the installation of the Doop environment and the collaboration of Jasmine.

* Modify Main.java in **soot-fact-generator** of the Doop framework

```sh
$ vim newdoop/generators/soot-fact-generator/src/main/java/org/clyze/doop/soot/Main.java
```

* Add the path of bean.xml

```java
288:        CreateEdge createEdge = new CreateEdge();
289:        createEdge.initCallGraph("/root/0610newtest/src/SpringDemo/bean.xml");
```

#### 1.1.1 JackEE, Default and Jasmine use context-insensitive analysis (corresponding to ID-1-EntryPoint, ID-4-SpringAOP, ID-7-Reachable  Methods, ID-8-Application Edges)

```sh
$ cd newdoop
# JackEE
$ ./doop -a context-insensitive --id SpringDemo -i /root/0610newtest/src/SpringDemo/ --open-programs jackee --platform java_8 --souffle-jobs 30 -t 3600 

# Defalut
$ ./doop -a context-insensitive --id SpringDemo -i /root/0610newtest/src/SpringDemo/ --platform java_8 --souffle-jobs 30 -t 3600

# Jasmine
$ ./doop -a context-insensitive --id SpringDemo -i /root/0610newtest/src/SpringDemo/ --open-programs jasmine --platform java_8 --souffle-jobs 30 -t 3600 
```

#### 1.1.2 JackEE, Default and Jasmine use context-insensitive analysis and taint analysis (corresponding to ID-2-DI(Singleton), ID-3-DI(Prototype), ID-5-InfoLeak)

* Replace /root/newdoop/souffle-logic/addons/information-flow/spring-sources-and-sinks.dl with spring-sources-and-sinks.dl in [Doop_Jasmine](https://github.com/SpringJasmine/Doop_Jasmine).

* Run the following command.

```sh
$ cd newdoop
# JackEE
$ ./doop -a 2-object-sensitive+heap --id SpringDemo -i /root/0610newtest/src/SpringDemo/ -l /root/0610newtest/libs/SpringDemo/lib --open-programs jackee --information-flow spring --platform java_8 --souffle-jobs 30 -t 3600 

# Defalut
$ ./doop -a 2-object-sensitive+heap --id SpringDemo -i /root/0610newtest/src/SpringDemo/ -l /root/0610newtest/libs/SpringDemo/lib --information-flow spring --platform java_8 --souffle-jobs 30 -t 3600

# Jasmine
$ ./doop -a 2-object-sensitive+heap --id SpringDemo -i /root/0610newtest/src/SpringDemo/ -l /root/0610newtest/libs/SpringDemo/lib --open-programs jasmine --information-flow spring --platform java_8 --souffle-jobs 30 -t 3600
```

### 1.2 Soot version comparison

 This project mainly uses maven and IntelliJ IDEA, the maven version is 3.6.1, and the Java version is Java 8.

#### 1.2.1 CHA, SPARK and Jasmine（corresponding to ID-1-EntryPoint, ID-4-SpringAOP, ID-7-Reachable  Methods, ID-8-Application Edges）

* Pull Jasmine from Github

```sh
$ git clone https://github.com/SpringJasmine/Doop_Jasmine.git
```

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

#### 1.2.2 SPARK, Jasmine context-sensitive（corresponding to ID-2-DI(Singleton), ID-3-DI(Prototype)）

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

## 2.Tab TABLE III

This table mainly shows the improvement of Jasmine's detection ability of JackEE, including the acquisition of all data and charts in Section 5.3 of the paper, mainly including TABLE III, TABLE IV, Fig.5 and Fig.6. Due to differences in the performance of the relevant machines, there may be some minor differences in the correlation results.

### 2.1. Docker

Please install the appropriate version of Docker and build the base container by following the commands below.

```sh
$ docker pull ubuntu:16.04
$ docker run -it -h Jasmine.gt --privileged=True -e "container=docker" -v /localdata:/data --net=host --shm-size="1g" --name="Jasmine" ubuntu:16.04 bash
# /localdata is your local folder path
```

### 2.2 Environment

Doop framework runtime requires Java, Datalog engine, etc. In order to make the Doop framework run smoothly, the following describes how to prepare the relevant operating environment.

* After entering the docker container, make sure to install related tools under the root user.

```sh
$ cd /root
$ apt-get update
$ apt-get install wget
$ apt-get install unzip
$ apt-get install vim
```

* Install Java and gradle

```sh
$ apt install software-properties-common
$ add-apt-repository ppa:openjdk-r/ppa
$ apt-get update
$ apt-get install openjdk-8-jdk
$ java -version

$ wget https://services.gradle.org/distributions/gradle-6.4-bin.zip
$ unzip -d /usr/local/ gradle-6.4-bin.zip
$ vim /etc/profile
```

Add the following two lines to ```/etc/profile```

```sh
export GRADLE_HOME=/usr/local/gradle-6.4
export PATH=$GRADLE_HOME/bin:$PATH
```

The configuration takes effect after saving

```sh
$ source /etc/profile
$ gradle -v
```

* Install Soufflé. Souffle1.5.1 version used in the paper

```sh
$ wget https://github.com/souffle-lang/souffle/releases/download/1.5.1/souffle_1.5.1-1_amd64.deb

$ apt-get install \
  bison \
  build-essential \
  clang \
  cmake \
  doxygen \
  flex \
  g++ \
  git \
  libffi-dev \
  libncurses5-dev \
  libsqlite3-dev \
  make \
  mcpp \
  python \
  sqlite \
  zlib1g-dev 
  
$ dpkg -i souffle_1.5.1-1_amd64.deb
$ souffle
```

### 2.3 Doop and Jasmine

Pull the Doop framework (https://bitbucket.org/yanniss/doop-benchmarks/src/master/) and doop_benchmarks that have been adapted to Jasmine.

```sh
$ git clone https://github.com/SpringJasmine/Doop_Jasmine.git
$ git clone https://bitbucket.org/yanniss/doop-benchmarks.git
```

If the compressed package (doop.tar.gz) pulled by git cannot be used, use the following command

```sh
$ wget https://github.com/SpringJasmine/Doop_Jasmine/raw/main/doop/doop.tar.gz
```

Switch to the user directory and move the relevant files to the /root directory

```sh
$ mv ~/Doop_Jasmine/0610newtest/ /root/
$ tar -zxvf doop.tar.gz
$ mv ~/Doop_Jasmine/doop/newdoop/ /root/
```

Set doop environment variables

```sh
export DOOP_HOME=/root/newdoop
export DOOP_OUT=/data/doop/out
export DOOP_CACHE=/data/doop/cache
export DOOP_TMP=/data/doop/tmp
export DOOP_LOG=/data/doop/log
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
export DOOP_PLATFORMS_LIB=/root/doop-benchmarks-docker
```

make the configuration take effect

```sh
$ source /etc/profile
```

### 2.4 Run Jasmine and JackEE

The following code describes how to enable and disable Jasmine

* First use the following command to enter the Main file in the **soot-fact-generator** of the Doop framework

```sh
$ vim newdoop/generators/soot-fact-generator/src/main/java/org/clyze/doop/soot/Main.java
```

* Comment the following statement to close Jasmine, uncomment it to open Jasmine

```java
288:        CreateEdge createEdge = new CreateEdge();
289:        createEdge.initCallGraph("");
```

Run the following python script to make Jas_JackEE and JackEE analyze the Production-benchmark.

```sh
$ python3 runjasmine.py
$ python3 runjackee.py
```

### 2.5 Run data processing scripts

The script configuration file is written as follows, among which Jasmine_CallGraph_Path, JackEE_CallGraph_Path, Jasmine_ReachableMethod_Path, JackEE_ReachableMethod_Path should be written according to the location of the data file generated by the script in the above section.

```properties
process_project_name = favorites-web
Jasmine_CallGraph_Path = /Users/favorites-web-jasmine/CallGraphEdge.csv
JackEE_CallGraph_Path = /Users/favorites-web-jackee/CallGraphEdge.csv
Jasmine_ReachableMethod_Path = /Users/favorites-web-jasmine/Stats_Simple_Application_ReachableMethod.csv
JackEE_ReachableMethod_Path = /Users/favorites-web-jackee/Stats_Simple_Application_ReachableMethod.csv
resultPath = output/
```

Run the data processing script, and its specific results will be displayed in the above configuration file.

```sh
$ java -jar DataCollect-1.0-SNAPSHOT.jar -c /Users/analysis/resources/config.properties
```

## 3. Tab TABLE V

This table mainly shows the improvement of Jasmine's detection ability of FlowDroid, including the acquisition of all data and charts in Section 5.4 of the paper, mainly including TABLE V. This project mainly uses maven and IntelliJ IDEA, the maven version is 3.6.1, and the Java version is Java 8.

### 3.1 Environment

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

### 3.2 run FlowDroid

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
