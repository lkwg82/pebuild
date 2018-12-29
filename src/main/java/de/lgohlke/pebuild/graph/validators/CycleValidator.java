package de.lgohlke.pebuild.graph.validators;

import de.lgohlke.pebuild.StepExecutor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CycleValidator {
    public static void validate(Collection<StepExecutor> jobs) {
        for (StepExecutor job : jobs) {
            visitJob(job, new HashSet<>(), job);
        }
    }

    private static void visitJob(StepExecutor job, Set<StepExecutor> visitedJobs, StepExecutor entryJob) {
        if (job.equals(entryJob) && !visitedJobs.isEmpty()) {
            String jobString = visitedJobs.stream()
                                          .map(StepExecutor::getName)
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
