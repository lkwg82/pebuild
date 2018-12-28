package de.lgohlke.ci;

import de.lgohlke.ci.config.dto.Step;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class StepExecutorConverter {
    private final Step step;

    public ShellExecutor asShellExecutor() {
        return new ShellExecutor(step.getCommand(), step.getTimeoutAsDuration());
    }
}
