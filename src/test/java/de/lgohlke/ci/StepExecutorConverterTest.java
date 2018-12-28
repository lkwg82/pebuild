package de.lgohlke.ci;

import de.lgohlke.ci.config.dto.Step;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepExecutorConverterTest {
    @Test
    void shouldConvertToShellExecutor() {
        Step step = new Step();
        step.setName("demo");
        step.setCommand("date");
        step.setTimeout("1m");

        FinishNotifier finishNotifier = new FinishNotifier("demo");

        StepExecutor executor = new StepExecutorConverter(step, finishNotifier).asShellExecutor();

        assertThat(executor.getCommand()).isEqualTo(step.getCommand());
        assertThat(executor.getTimeout()).isEqualTo(step.getTimeoutAsDuration());
        assertThat(executor.getFinishNotifier()).isEqualTo(finishNotifier);
    }
}