package de.lgohlke.ci;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Duration;

@RequiredArgsConstructor
@Getter
@ToString
public abstract class StepExecutor {
    private final String command;
    private final Duration timeout;

    private TimeContext timeContext = new TimeContext(0, 0);

    public final void execute() {

        if (timeContext.endTimeMillis != 0) {
            throw new IllegalStateException("this step already ran: " + this);
        }

        long start = System.currentTimeMillis();
        try {
            runCommand();
        } catch (Throwable t) {
            // TODO
        }
        long end = System.currentTimeMillis();

        timeContext = new TimeContext(start, end);
    }

    public void runCommand() {
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
