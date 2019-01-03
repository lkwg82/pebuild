package de.lgohlke.pebuild;

import de.lgohlke.pebuild.config.dto.Step;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepExecutorConverterTest {
    @Test
    void shouldConvertToShellExecutor() {
        Step step = new Step();
        step.setName("demo");
        step.setCommand("date");
        step.setTimeout("1m");

        JobTrigger jobTrigger = new JobTrigger("demo");

        StepExecutor executor = new StepExecutorConverter(step, jobTrigger).asShellExecutor();

        assertThat(executor.getCommand()).isEqualTo(step.getCommand());
        assertThat(executor.getTimeout()).isEqualTo(step.getTimeoutAsDuration());
        assertThat(executor.getJobTrigger()).isEqualTo(jobTrigger);
    }
}