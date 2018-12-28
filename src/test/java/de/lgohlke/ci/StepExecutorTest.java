package de.lgohlke.ci;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StepExecutorTest {
    private StepExecutor executor = new StepExecutor("", Duration.ZERO) {
    };

    @Test
    void shouldHaveZeroInitialisedTimeContext() {

        assertThat(executor.getTimeContext()
                           .getStartTimeMillis()).isZero();
        assertThat(executor.getTimeContext()
                           .getEndTimeMillis()).isZero();
    }

    @Test
    void shouldHaveTimeContextWithSomeNumbers() {

        executor.execute();

        assertThat(executor.getTimeContext()
                           .getStartTimeMillis()).isBetween(System.currentTimeMillis() - 1000,
                                                            System.currentTimeMillis());
        assertThat(executor.getTimeContext()
                           .getEndTimeMillis()).isBetween(System.currentTimeMillis() - 1000,
                                                          System.currentTimeMillis());
    }

    @Test
    void shouldFailOn2ndExecution() {
        executor.execute();
        assertThrows(IllegalStateException.class, () -> executor.execute());
    }
}