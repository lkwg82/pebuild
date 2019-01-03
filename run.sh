#!/usr/bin/env bash

set -ex

mvn clean package -DskipTests

scripts/compile-to-binary.sh &
mvn verify &

wait $(jobs -p)