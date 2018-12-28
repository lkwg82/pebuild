package de.lgohlke.ci.graph;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class Job {
    private final String name;
    private final Set<Job> waitForJobs = new HashSet<>();

    public void waitFor(Job job) {
        waitForJobs.add(job);
    }

    @Override
    public String toString() {
        String dependsOn = "[" + waitForJobs.stream()
                                            .sorted(Comparator.comparing(o -> o.getName()
                                                                               .toLowerCase()))
                                            .map(Job::getName)
                                            .collect(Collectors.joining(",")) + "]";
        return name + " " + dependsOn;
    }
}
