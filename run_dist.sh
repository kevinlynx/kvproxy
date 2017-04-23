#!/bin/sh
java -cp kvproxy-1.0-SNAPSHOT.jar:lib/* -Dlog4j.configuration=file:log4j.properties \
    -Xloggc:gc.log -XX:+PrintGCTimeStamps -XX:+PrintGCDetails \
        com.codemacro.kvproxy.App
