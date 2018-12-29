#!/usr/bin/env bash

set -e

if [[ ! -f .graalvm/graalvm-ce.tar.gz ]]; then
    mkdir -p .graalvm
    wget -O .graalvm/graalvm-ce.tar.gz https://github.com/oracle/graal/releases/download/vm-1.0.0-rc10/graalvm-ce-1.0.0-rc10-linux-amd64.tar.gz
    tar -xvzf .graalvm/graalvm-ce.tar.gz -C ./.graalvm
fi
localGraalVMBin=$(find .graalvm/ -maxdepth 1 -type d | tail -1)
export PATH=$PWD/$localGraalVMBin/bin:$PATH

mvn clean verify

classPathOfJar=$(find target/classes/lib -type f| sort | xargs | tr ' ' ':')

native-image \
    --no-server \
    --static \
    --class-path target/classes:${classPathOfJar} \
    -H:ReflectionConfigurationFiles=graalvm.reflections.json \
    -H:Path=target \
    -H:Name="pbuild" \
    de.lgohlke.ci.Main

# only for release
# upx -v target/pbuild

/usr/bin/time -v ./target/pbuild date

if [[ $? == 0 ]]; then
    echo "build ok"
fi