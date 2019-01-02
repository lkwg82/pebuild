#!/usr/bin/env bash

set -ex

mvn clean verify

unamestr=`uname`
if [[ ! -f .graalvm/graalvm-ce.tar.gz ]]; then
    mkdir -p .graalvm
    if [[ "$unamestr" == 'Linux' ]]; then
      url=https://github.com/oracle/graal/releases/download/vm-1.0.0-rc10/graalvm-ce-1.0.0-rc10-linux-amd64.tar.gz
    elif [[ "$unamestr" == 'Darwin' ]]; then
      url=https://github.com/oracle/graal/releases/download/vm-1.0.0-rc10/graalvm-ce-1.0.0-rc10-macos-amd64.tar.gz
    fi

    wget -O .graalvm/graalvm-ce.tar.gz ${url}
    tar -xzf .graalvm/graalvm-ce.tar.gz -C ./.graalvm
fi

if [[ "$unamestr" == 'Linux' ]]; then
  url=https://github.com/oracle/graal/releases/download/vm-1.0.0-rc10/graalvm-ce-1.0.0-rc10-linux-amd64.tar.gz
  localGraalVMBin=$(find .graalvm/ -maxdepth 1 -type d | tail -1)
elif [[ "$unamestr" == 'Darwin' ]]; then
  url=https://github.com/oracle/graal/releases/download/vm-1.0.0-rc10/graalvm-ce-1.0.0-rc10-macos-amd64.tar.gz
  localGraalVMBin=$(find .graalvm/ -maxdepth 1 -type d | tail -1)/Contents/Home
fi
export PATH=$PWD/${localGraalVMBin}/bin:$PATH

# wait to not interfere with ide
sleep 3

classPathOfJar=$(find target/classes/lib -type f| sort | xargs | tr ' ' ':')

set -x
native-image \
    --no-server \
    --static \
    --class-path target/classes:${classPathOfJar} \
    -H:+ReportUnsupportedElementsAtRuntime \
    -H:ReflectionConfigurationFiles=graalvm.reflections.json \
    -H:Path=target \
    -H:Name="pebuild" \
    de.lgohlke.pebuild.Main
set +x


# only for release
# upx -v target/pbuild

/usr/bin/time -v ./target/pebuild date

if [[ $? == 0 ]]; then
    echo "build ok"
fi
