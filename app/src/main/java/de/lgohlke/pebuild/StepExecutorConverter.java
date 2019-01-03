package de.lgohlke.pebuild;

import de.lgohlke.pebuild.config.dto.Step;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class StepExecutorConverter {
    private final Step step;
    private final JobTrigger jobTrigger;

    public ShellExecutor asShellExecutor() {
        return new ShellExecutor(step.getName(), step.getCommand(), step.getTimeoutAsDuration(), jobTrigger);
    }
}
