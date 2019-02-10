#!/usr/bin/env bash

set -e

shopt -s expand_aliases



function t {
    local name=$1
    local bin=$2
    local expectedExitCode=${3:-0}

    local RED='\033[0;31m'
    local GREEN='\033[0;32m'
    local BLUE='\033[0;34m'
    local NC='\033[0m' # No Color

    printf "${BLUE}TEST${NC} '$name' "

    local out=$(mktemp)
    local err=$(mktemp)

    set +e
    eval ${bin} >${out} 2>${err}
    local exitCode=$?
    set -e

    if [[ ${exitCode} == ${expectedExitCode} ]]; then
        printf "${GREEN}ok${NC}\n"
    else
        printf "${RED}fail${NC}\n"
        echo
        echo "OUTPUT: "
        cat ${out} ${err}
    fi
}

for binary in scripts/runJar.sh target/pebuild; do

    echo "-------"
    echo "testing with ${binary}"
    echo "-------"

    alias bin=${binary}

    t "help" "bin -h"
    t "exec date" "bin -d exec date"
    t "exit 1" "bin exec exit 1" 1
    t "run --dry-run --file integration/simple.pbuild.yml"

done