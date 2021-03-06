#!/usr/bin/env bash

set -ex

unamestr=`uname`
if [[ ! -f .graalvm/graalvm-ce.tar.gz ]]; then
    mkdir -p .graalvm
    if [[ "$unamestr" == 'Linux' ]]; then
      url=https://github.com/oracle/graal/releases/download/vm-1.0.0-rc11/graalvm-ce-1.0.0-rc11-linux-amd64.tar.gz
    elif [[ "$unamestr" == 'Darwin' ]]; then
      url=https://github.com/oracle/graal/releases/download/vm-1.0.0-rc11/graalvm-ce-1.0.0-rc11-macos-amd64.tar.gz
    fi

    wget -O .graalvm/graalvm-ce.tar.gz ${url}
    tar -xzf .graalvm/graalvm-ce.tar.gz -C ./.graalvm
fi

if [[ "$unamestr" == 'Linux' ]]; then
  localGraalVMBin=$(find .graalvm/ -maxdepth 1 -type d | tail -1)
elif [[ "$unamestr" == 'Darwin' ]]; then
  localGraalVMBin=$(find .graalvm/ -maxdepth 1 -type d | tail -1)/Contents/Home
fi
export PATH=$PWD/${localGraalVMBin}/bin:$PATH

classPathOfJar=$(find app/target/classes/lib -type f| sort | xargs | tr ' ' ':')
mkdir -p target

# osx can not build static
native-image \
    $([[ "$unamestr" == 'Linux' ]] && echo -n "--static" || echo -n "") \
    --no-server \
    --class-path app/target/classes:${classPathOfJar} \
    -H:+ReportUnsupportedElementsAtRuntime \
    -H:ReflectionConfigurationFiles=lib.config/graalvm.reflections.json,app/graalvm.reflection.json \
    -H:Path=target \
    -H:Name="pebuild" \
    de.lgohlke.pebuild.Main

# only for release
# upx -v target/pbuild

if [[ "$unamestr" == 'Linux' ]]; then
  /usr/bin/time -v ./target/pebuild -h
elif [[ "$unamestr" == 'Darwin' ]]; then
  /usr/bin/time -lp ./target/pebuild -h
fi

if [[ $? == 0 ]]; then
    echo "build ok"
fi