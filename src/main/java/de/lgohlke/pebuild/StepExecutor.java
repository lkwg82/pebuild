package de.lgohlke.pebuild;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@ToString
@Slf4j
public abstract class StepExecutor {
    private final @NonNull String name;
    private final @NonNull String command;
    private final @NonNull Duration timeout;
    private final @NonNull JobTrigger jobTrigger;

    private final Set<StepExecutor> waitForJobs = new HashSet<>();
    private TimeContext timeContext = new TimeContext(0, 0);

    public final void execute() {

        if (timeContext.endTimeMillis != 0) {
            throw new IllegalStateException("this step already ran: " + this);
        }

        long start = System.currentTimeMillis();
        try {
            log.info("command:" + command);
            runCommand();
            log.info("completed command {}", command);
            long end = System.currentTimeMillis();
            timeContext = new TimeContext(start, end);

            jobTrigger.triggerCompletion(timeContext);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void runCommand() throws Exception {
        // TODO
    }

    public void waitFor(StepExecutor executor) {
        if (!waitForJobs.add(executor)) {
            throw new IllegalArgumentException("tried to add this executor again: " + executor);
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

    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class TimeContext {
        private final long startTimeMillis;
        private final long endTimeMillis;
    }
}
