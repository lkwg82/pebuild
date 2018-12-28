package de.lgohlke.ci.graph.validators;

import de.lgohlke.ci.graph.Job;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class ReferencedJobMissingValidator {
    public static void validate(Collection<Job> jobs) {
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

    public static class ReferencedJobsMissing extends RuntimeException {
        ReferencedJobsMissing(String message) {
            super(message);
        }
    }
}
