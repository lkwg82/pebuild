**summary**

simple powerfull parallel build tool

**design goals**

- simplicity
- robustness

**features**
- (TODO) yaml-like build file
- (TODO) maximal parallel builds
- (TODO) timeout for build steps
- (TODO) limited and low complexity build language
* (TODO) debuggability:
  - (TODO) shows expected execution graph
  - (TODO) shows output of build steps


example:

```yaml

options:
- timeout: 10m
  
steps:
- name: demo
  command: 'date'
- name: sleep
  command: 'sleep 2'
  timeout: 10s
  waitfor: ['demo']
```