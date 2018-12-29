package de.lgohlke.pebuild.graph.validators;

import de.lgohlke.pebuild.StepExecutor;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class ReferencedJobMissingValidator {
    public static void validate(Collection<StepExecutor> jobs) {
        Set<StepExecutor> allWaitJobs = jobs.stream()
                                            .map(StepExecutor::getWaitForJobs)
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.toSet());

        allWaitJobs.removeAll(jobs);

        if (!allWaitJobs.isEmpty()) {
            String jobString = allWaitJobs.stream()
                                          .map(StepExecutor::getName)
                                          .collect(Collectors.joining(","));
            throw new ReferencedJobsMissing("referenced jobs are missing: " + jobString);
        }
    }

    public static class ReferencedJobsMissing extends RuntimeException {
        public ReferencedJobsMissing(String message) {
            super(message);
        }
    }
}
