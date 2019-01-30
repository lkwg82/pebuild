package de.lgohlke.pebuild.graph;

import de.lgohlke.pebuild.Configuration;
import de.lgohlke.pebuild.ExecutionResult;
import de.lgohlke.pebuild.StepExecutor;
import de.lgohlke.pebuild.TimingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionGraphTest {
    @BeforeEach
    void setUp() {
        Configuration.configureDefaults();
    }

    @Test
    void printGraph() {
        StepExecutor a = createStepExecutor("A", 0);
        StepExecutor b = createStepExecutor("B", 0);
        b.waitFor(a);

        ExecutionGraph graph = createGraph(Duration.ofMinutes(10), a, b);

        assertThat(graph.toString()).isEqualTo("(A [])->(B [A])");
    }

    @Test
    void shouldFailOnDuplicatedJob() {
        StepExecutor a = createStepExecutor("A", 0);

        ExecutionGraph.Builder builder = new ExecutionGraph.Builder().addJob(a);

        assertThrows(ExecutionGraph.Builder.DuplicateJobException.class, () -> builder.addJob(a));
    }

    //   @RepeatedTest(10)
    void shouldExecuteInRightOrder() {
        StepExecutor a = createStepExecutor("A", 10);
        StepExecutor b = createStepExecutor("B", 10);

        b.waitFor(a);

        createGraph(Duration.ofMinutes(10), a, b).execute();

        TimingContext timingContextA = a.getTimingContext();
        TimingContext timingContextB = b.getTimingContext();

        long endTimeMillisA = timingContextA.getEndTimeMillis();
        long startTimeMillisB = timingContextB.getStartTimeMillis();

        assertThat(startTimeMillisB).isGreaterThanOrEqualTo(endTimeMillisA);
    }

    @Disabled
    @Test
    void shouldWaitForLastJobFinished() {
        StepExecutor a = createStepExecutor("A", 200);

        createGraph(Duration.ofMinutes(10), a).execute();

        long endTimeMillisA = a.getTimingContext()
                               .getEndTimeMillis();
        assertThat(endTimeMillisA).isGreaterThan(0);
    }

    @Test
    void shouldTimeoutWhenJobExceeds() {
        long start = System.currentTimeMillis();

        StepExecutor a = createStepExecutor("A", 2000);

        createGraph(Duration.ofMillis(200), a).execute();

        long end = System.currentTimeMillis();

        assertThat(end - start).isBetween(150L, 350L);
    }

    private static ExecutionGraph createGraph(Duration timeout, StepExecutor... executors) {
        ExecutionGraph.Builder builder = new ExecutionGraph.Builder().timeout(timeout);
        for (StepExecutor j : executors) {
            builder.addJob(j);
        }
        ExecutionGraph graph = builder.build();

//        graph.getJobs()
//             .forEach(j -> j.getJobTrigger()
//                            .registerHandler(graph));
        return graph;
    }

    private static StepExecutor createStepExecutor(String name, int delay) {
        return new StepExecutor(name, "cmd " + name) {
            @Override
            public ExecutionResult runCommand() throws Exception {
                TimeUnit.MILLISECONDS.sleep(delay);
                return new ExecutionResult(0);
            }
        };
    }
}
