#!/bin/sh

set -eu

mvn exec:java -Dexec.mainClass="designgrapher.Main" -Dexec.args="/home/richard/Projects/jMSR/jmsr-daemon/target/classes"
dot output.dot -Tpng > output.png
firefox output.png
