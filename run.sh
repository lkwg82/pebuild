#!/usr/bin/env bash

set -ex

mvn clean verify
scripts/compile-to-binary.sh