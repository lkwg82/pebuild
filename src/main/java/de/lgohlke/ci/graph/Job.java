package de.lgohlke.ci.graph;

import de.lgohlke.ci.FinishNotifier;
import de.lgohlke.ci.StepExecutor;
import lombok.Getter;
import lombok.NonNull;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Job {
    @Getter
    private final String name;
    @Getter
    private final StepExecutor executor;

    private final Set<Job> waitForJobs = new HashSet<>();

    @Deprecated
    public Job(@NonNull String name) {
        this(name, new StepExecutor("test", Duration.ZERO, new FinishNotifier("test")) {
        });
    }

    public Job(@NonNull String name, @NonNull StepExecutor executor) {
        this.name = name;
        this.executor = executor;
    }

    public void waitFor(Job job) {
        if (!waitForJobs.add(job)) {
            throw new IllegalArgumentException("tried to add this job again: " + job);
        }
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

    public Set<Job> getWaitForJobs() {
        return new HashSet<>(waitForJobs);
    }
}
