package de.lgohlke.ci;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@RequiredArgsConstructor
@Getter
@ToString
@Slf4j
public abstract class StepExecutor {
    private final @NonNull String command;
    private final @NonNull Duration timeout;
    private final @NonNull JobTrigger jobTrigger;

    private TimeContext timeContext = new TimeContext(0, 0);

    public final void execute() {

        if (timeContext.endTimeMillis != 0) {
            throw new IllegalStateException("this step already ran: " + this);
        }

        long start = System.currentTimeMillis();
        try {
            log.info("command:" + command);
            runCommand();
            long end = System.currentTimeMillis();
            timeContext = new TimeContext(start, end);

            jobTrigger.triggerCompletion();
        } catch (Throwable t) {
            // TODO
        }
    }

    public void runCommand() throws Exception {
        // TODO
    }

    @RequiredArgsConstructor
    @Getter
    @ToString
    public class TimeContext {
        private final long startTimeMillis;
        private final long endTimeMillis;
    }
}
