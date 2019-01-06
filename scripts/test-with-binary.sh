#!/usr/bin/env bash

set -e

shopt -s expand_aliases


alias bin='target/pebuild'

function t {
    local name=$1
    local bin=$2

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

    if [[ ${exitCode} == "0" ]]; then
        printf "${GREEN}ok${NC}\n"
    else
        printf "${RED}fail${NC}\n"
        echo
        echo "OUTPUT: "
        cat ${out} ${err}
    fi
}

t "help" "bin -h"
t "help" "bin -d exec date"