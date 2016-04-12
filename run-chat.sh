#!/bin/bash

mvn compile
mvn exec:java -Dexec.mainClass="org.swellrt.java.examples.SimpleChat" -Dexec.classpathScope=runtime -Dexec.args="http://demo.swellrt.org demo.swellrt.org"