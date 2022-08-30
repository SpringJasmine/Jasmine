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



The results of **TABLE III and TABLE IV** can be counted by executing the following commands.

```sh
$ java -jar /root/DataCollect-1.0-SNAPSHOT.jar -c /root/config.properties
```
