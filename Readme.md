**summary**

simple powerfull parallel build execution tool

**design goals**

- simplicity
- robustness

**features**
- (TODO) yaml-like config file
- (TODO) maximal parallel executions
- (TODO) timeout for execution steps
- (TODO) limited and low complexity config language
* (TODO) debuggability:
  - (TODO) shows expected execution graph
  - (TODO) shows output of execution steps


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