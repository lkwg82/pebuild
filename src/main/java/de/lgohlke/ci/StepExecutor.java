package de.lgohlke.ci;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
@Getter
public abstract class StepExecutor {
    private final String command;
    private final Duration timeout;

    private TimeContext timeContext = new TimeContext(0, 0);

    public final void execute() {
        long start = System.currentTimeMillis();
        try {
            runCommand();
        } catch (Throwable t) {
            // TODO
        }
        long end = System.currentTimeMillis();

        timeContext = new TimeContext(start, end);
    }

    private void runCommand() {
        // TODO
    }

    @RequiredArgsConstructor
    @Getter
    public class TimeContext {
        private final long startTimeMillis;
        private final long endTimeMillis;
    }
}
