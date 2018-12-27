#!/usr/bin/env bash

set -e

if [[ ! -f .graalvm/graalvm-ce.tar.gz ]]; then
    mkdir -p .graalvm
    wget -O .graalvm/graalvm-ce.tar.gz https://github.com/oracle/graal/releases/download/vm-1.0.0-rc10/graalvm-ce-1.0.0-rc10-linux-amd64.tar.gz
    tar -xvzf .graalvm/graalvm-ce.tar.gz -C ./.graalvm
fi
localGraalVMBin=$(find .graalvm/ -maxdepth 1 -type d | tail -1)
export PATH=$PWD/$localGraalVMBin/bin:$PATH

mvn clean compile

native-image --no-server --static -cp target/classes de.lgohlke.ci.Main
upx -9v de.lgohlke.ci.main

./de.lgohlke.ci.main date

if [[ $? == 0 ]]; then
    echo "build ok"
fi