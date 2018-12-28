package de.lgohlke.ci;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
@Getter
public abstract class StepExecutor {
    private final String command;
    private final Duration timeout;
}
