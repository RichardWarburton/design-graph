#!/bin/sh

set -eu

mvn exec:java -Dexec.mainClass="designgrapher.Main" -Dexec.args="$1"
fdp output.dot -Tpng > output.png
firefox output.png
