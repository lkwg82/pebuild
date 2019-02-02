#!/usr/bin/env bash

set -e

if [[ ! -d app/target/classes/lib ]]; then
     echo 'ERROR run mvn package -DskipTests'
     exit 1
 fi

classPathOfJar=$(find app/target/classes/lib -type f| sort | xargs | tr ' ' ':')
java --class-path app/target/classes:${classPathOfJar} de.lgohlke.pebuild.Main "$@"