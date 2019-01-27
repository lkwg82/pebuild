package de.lgohlke.pebuild;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StepExecutorTest {
    private StepExecutor executor = new StepExecutor("name", "") {
    };

    @Test
    void shouldHaveZeroInitialisedTimeContext() {

        assertThat(executor.getTimingContext()
                           .getStartTimeMillis()).isZero();
        assertThat(executor.getTimingContext()
                           .getEndTimeMillis()).isZero();
    }

    @Test
    void shouldHaveTimeContextWithSomeNumbers() {

        executor.execute();

        assertThat(executor.getTimingContext()
                           .getStartTimeMillis()).isBetween(System.currentTimeMillis() - 1000,
                                                            System.currentTimeMillis());
        assertThat(executor.getTimingContext()
                           .getEndTimeMillis()).isBetween(System.currentTimeMillis() - 1000,
                                                          System.currentTimeMillis());
    }

    @Test
    void shouldFailOn2ndExecution() {
        executor.execute();
        assertThrows(IllegalStateException.class, () -> executor.execute());
    }

    @Nested
    class finishNotification {

        //@Test
        // TODO
        void shouldNotifyCompletion() {
            LongAdder counter = new LongAdder();
            /*JobTrigger jobTrigger = new JobTrigger("test") {

                @Override
                public void triggerCompletion(TimingContext timeContext) {
                    counter.increment();
                }
            };*/

            StepExecutor executor = new StepExecutor("name", "") {
            };

            executor.execute();

            assertThat(counter.longValue()).isEqualTo(1L);
        }
    }

    @Test
    void failOnDuplicateWaitForStep() {
        StepExecutor a = new StepExecutor("A", "") {
        };
        StepExecutor b = new StepExecutor("B", "") {
        };

        a.waitFor(b);

        assertThrows(IllegalArgumentException.class, () -> a.waitFor(b));
    }
}