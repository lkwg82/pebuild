#!/usr/bin/env bash

set -ex

mvn clean verify
scripts/compile-to-binary.sh
scripts/test-with-binary.sh