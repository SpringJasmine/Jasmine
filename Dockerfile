# Version 1.0
FROM ubuntu:16.04

LABEL maintainer = "SpringJasmine"

RUN apt-get update && apt install software-properties-common -y && add-apt-repository ppa:openjdk-r/ppa \ 
&& apt-get update && apt-get install vim -y && apt-get install wget -y 

RUN apt-get install unzip -y && apt-get install openjdk-8-jdk -y \
&& apt-get install -y \
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

RUN wget https://services.gradle.org/distributions/gradle-6.4-bin.zip \
&& unzip -d /usr/local/ gradle-6.4-bin.zip \
&& wget https://github.com/souffle-lang/souffle/releases/download/1.5.1/souffle_1.5.1-1_amd64.deb \
&& dpkg -i souffle_1.5.1-1_amd64.deb \
&& git clone https://github.com/SpringJasmine/Doop_Jasmine.git \
&& git clone https://bitbucket.org/yanniss/doop-benchmarks.git \
&& wget https://github.com/SpringJasmine/Doop_Jasmine/raw/main/doop/doop.tar.gz 

RUN mv /Doop_Jasmine/0610newtest/ /root && tar -zxvf doop.tar.gz && mv /newdoop/ /root && mv /doop-benchmarks/ /root \
&& mv /Doop_Jasmine/DataCollect-1.0-SNAPSHOT.jar /root && mv /Doop_Jasmine/bashDir/* /root/ \
&& mv /Doop_Jasmine/spring-sources-and-sinks.dl /root/newdoop/souffle-logic/addons/information-flow/ \
&& rm -rf /Doop_Jasmine && rm -rf doop.tar.gz && rm -rf gradle-6.4-bin.zip && rm -rf souffle_1.5.1-1_amd64.deb 

RUN touch /root/newdoop/souffle-logic/analyses/context-insensitive/../../main/main-declarations.dl \
&& echo 'process_project_name = mall-admin\n\
Jasmine_CallGraph_Path = /data/doop/out/jasminemall-admin/database/CallGraphEdge.csv\n\
JackEE_CallGraph_Path = /data/doop/out/jackeemall-admin/database/CallGraphEdge.csv\n\
Jasmine_ReachableMethod_Path = /data/doop/out/jasminemall-admin/database/Stats_Simple_Application_ReachableMethod.csv\n\
JackEE_ReachableMethod_Path = /data/doop/out/jackeemall-admin/database/Stats_Simple_Application_ReachableMethod.csv\n\
resultPath = /root/output/' >> /root/config.properties \
&& sed -i 70d /root/newdoop/runjackee.py && sed -i 70d /root/newdoop/runjasmine.py \
&& sed -i 352d /root/newdoop/src/main/groovy/org/clyze/doop/core/DoopAnalysisFactory.groovy \
&& sed -i 380d /root/newdoop/src/main/groovy/org/clyze/doop/core/DoopAnalysisFactory.groovy \
&& sed -i 411d /root/newdoop/src/main/groovy/org/clyze/doop/core/DoopAnalysisFactory.groovy \
&& find . -name "*DS_Store" | xargs rm -r

ENV GRADLE_HOME=/usr/local/gradle-6.4 PATH="/usr/local/gradle-6.4/bin:${PATH}" DOOP_HOME=/root/newdoop DOOP_OUT=/data/doop/out \
DOOP_CACHE=/data/doop/cache DOOP_TMP=/data/doop/tmp DOOP_LOG=/data/doop/log JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8 \
DOOP_PLATFORMS_LIB=/root/doop-benchmarks


