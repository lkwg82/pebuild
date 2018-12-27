package de.lgohlke.ci.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class ExecutionGraph {
    private final Set<Job> jobs;

    private ExecutionGraph(Set<Job> jobs) {
        this.jobs = jobs;
    }

    @Override
    public String toString() {
        return jobs.stream()
                   .map(j -> "(" + j + ")")
                   .collect(Collectors.joining("->"));
    }

    public static class Builder {
        private Set<Job> jobs = new HashSet<>();

        Builder addJob(Job job) {
            jobs.add(job);
            return this;
        }

        void sort() {

            LinkedHashSet<Job> sortedJobs = new LinkedHashSet<>();

            Map<Job, Set<Job>> waitList = new LinkedHashMap<>();

            for (Job j : jobs) {
                waitList.put(j,
                             j.getWaitForJobs());
            }

            while (!waitList.isEmpty()) {
                List<Job> sortedCandidates = new ArrayList<>();
                waitList.forEach((j, waits) -> {
                    if (waits.isEmpty()) {
                        sortedCandidates.add(j);
                    }
                });

                // remove all zero waits
                sortedCandidates.forEach(waitList::remove);

                waitList.forEach((j, waits) -> {
                    Set<Job> waitForJobsCopy = new HashSet<>(waits);
                    if (waitForJobsCopy.removeAll(sortedCandidates)) {
                        waitList.put(j, waitForJobsCopy);
                    }
                });

                sortedJobs.addAll(sortedCandidates);
                sortedCandidates.clear();
            }

            jobs = sortedJobs;
        }

        void validate() {
            validateHaveAllJobsReferenced();
            validateNoCycle();
        }

        private void validateNoCycle() {

            for (Job job : jobs) {
                visitJob(job, new HashSet<>(), job);
            }
        }

        private void visitJob(Job job, Set<Job> visitedJobs, Job entryJob) {
            if (job.equals(entryJob) && !visitedJobs.isEmpty()) {
                String jobString = visitedJobs.stream()
                                              .map(Job::getName)
                                              .collect(Collectors.joining(","));
                throw new CycleDetected("cycle: " + jobString + " -> " + job.getName());
            }

            if (visitedJobs.add(job)) {
                job.getWaitForJobs()
                   .forEach(j -> visitJob(j, visitedJobs, entryJob));
            }
        }

        private void validateHaveAllJobsReferenced() {
            Set<Job> allWaitJobs = jobs.stream()
                                       .map(Job::getWaitForJobs)
                                       .flatMap(Collection::stream)
                                       .collect(Collectors.toSet());

            allWaitJobs.removeAll(jobs);

            if (!allWaitJobs.isEmpty()) {
                String jobString = allWaitJobs.stream()
                                              .map(Job::getName)
                                              .collect(Collectors.joining(","));
                throw new ReferencedJobsMissing("referenced jobs are missing: " + jobString);
            }
        }

        ExecutionGraph build() {

            validate();
            sort();

            return new ExecutionGraph(jobs);
        }

        class ReferencedJobsMissing extends RuntimeException {
            ReferencedJobsMissing(String message) {
                super(message);
            }
        }

        class CycleDetected extends RuntimeException {
            CycleDetected(String message) {
                super(message);
            }
        }
    }
}
