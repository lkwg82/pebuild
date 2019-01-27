package de.lgohlke.pebuild;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Slf4j
public abstract class StepExecutor {
    private final @NonNull String name;
    private final @NonNull String command;
    private final @NonNull Duration timeout;

    private final Set<StepExecutor> waitForJobs = new HashSet<>();
    private TimingContext timingContext = new TimingContext("unset", 0, 0);

    public StepExecutor(@NonNull String name,
                        @NonNull String command,
                        @NonNull Duration timeout) {
        this.name = name;
        this.command = command;
        this.timeout = timeout;
    }

    public final void execute() {

        if (timingContext.getEndTimeMillis() != 0) {
            throw new IllegalStateException("this step already ran: " + this);
        }

        long start = System.currentTimeMillis();
        try {
            log.info("command:" + command);
            runCommand();
            log.info("completed command {}", command);
            long end = System.currentTimeMillis();
            timingContext = new TimingContext(name, start, end);

            // TODO jobTrigger.triggerCompletion(timingContext);
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        }
    }

    public ExecutionResult runCommand() throws Exception {
        // TODO
        return null;
    }

    public void waitFor(@NonNull StepExecutor... executors) {
        for (StepExecutor executor : executors) {
            if (!waitForJobs.add(executor)) {
                throw new IllegalArgumentException("tried to add this executor again: " + executor);
            }
        }
    }

    @Override
    public String toString() {
        String dependsOn = "[" + waitForJobs.stream()
                                            .sorted(Comparator.comparing(o -> o.getName()
                                                                               .toLowerCase()))
                                            .map(StepExecutor::getName)
                                            .collect(Collectors.joining(",")) + "]";
        return name + " " + dependsOn;
    }

    public Set<StepExecutor> getWaitForJobs() {
        return new HashSet<>(waitForJobs);
    }

    public void cancel() {
        // TODO
        log.warn("cancel");
    }
}
