#!/bin/sh
java -cp kvproxy-1.0-SNAPSHOT.jar:lib/* -Dlog4j.configuration=file:log4j.properties com.codemacro.kvproxy.App
