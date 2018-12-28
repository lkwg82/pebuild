package de.lgohlke.ci.graph.validators;

import de.lgohlke.ci.graph.Job;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CycleValidator {
    public static void validate(Collection<Job> jobs) {
        for (Job job : jobs) {
            visitJob(job, new HashSet<>(), job);
        }
    }

    private static void visitJob(Job job, Set<Job> visitedJobs, Job entryJob) {
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

    static class CycleDetected extends RuntimeException {
        CycleDetected(String message) {
            super(message);
        }
    }
}
