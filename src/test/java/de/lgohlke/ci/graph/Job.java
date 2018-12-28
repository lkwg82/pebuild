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

@Getter
public class Job {
    private final @NonNull String name;
    private final @NonNull StepExecutor executor;
    private final @NonNull FinishNotifier finishNotifier;

    private final Set<Job> waitForJobs = new HashSet<>();

    @Deprecated
    public Job(String name) {
        this(name, new StepExecutor("test", Duration.ZERO) {
        }, new FinishNotifier() {
        });
    }

    public Job(String name, StepExecutor executor, FinishNotifier finishNotifier) {
        this.name = name;
        this.executor = executor;
        this.finishNotifier = finishNotifier;
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

}
