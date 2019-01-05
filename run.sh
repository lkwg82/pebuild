#!/usr/bin/env bash

set -ex

./mvnw clean verify
scripts/compile-to-binary.sh
scripts/test-with-binary.sh