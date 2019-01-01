[![Build Status](https://travis-ci.org/lkwg82/pebuild.svg?branch=master)](https://travis-ci.org/lkwg82/pebuild)

# PEBuild

Parallel Execution Build

**summary**

simple powerfull parallel build execution tool

**design goals**

- simplicity
- robustness
- easy integrateable into jenkins/travisci

**motivation**

Making the build fast and the configuration still very low in complexity.
I dont want to fine tune the concurrent steps. But I know what depends on what. 


minimal example:

```yaml
steps:
- name: demo
  command: 'date'
- name: sleep
  command: 'sleep 2'
  timeout: 10s
  waitfor: ['demo']
```

**features**
- (TODO) yaml-like config file
- (TODO) maximal parallel executions
- (TODO) timeout for execution steps
- (TODO) limited and low complexity config language
* (TODO) debuggability:
  - (TODO) shows expected execution graph
  - (TODO) shows output of execution steps

**inspiration**

- codeship (https://codeship.com)
- gcloud build (https://cloud.google.com/cloud-build)
- travisci (https://travis-ci.org)
