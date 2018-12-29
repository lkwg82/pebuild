package de.lgohlke.ci.graph;

import de.lgohlke.ci.JobTrigger;
import de.lgohlke.ci.StepExecutor;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionGraphTest {
    @Test
    void printGraph() {
        Job a = new Job("A");
        Job b = new Job("B");
        b.waitFor(a);

        ExecutionGraph graph = createGraph(Duration.ofMinutes(10), a, b);

        assertThat(graph.toString()).isEqualTo("(A [])->(B [A])");
    }

    @Test
    void shouldFailOnDuplicatedJob() {
        Job a = new Job("A");

        ExecutionGraph.Builder builder = new ExecutionGraph.Builder().addJob(a);

        assertThrows(ExecutionGraph.Builder.DuplicateJobException.class, () -> builder.addJob(a));
    }

    @RepeatedTest(10)
    void shouldExecuteInRightOrder() {
        Job a = createJob("A", 10);
        Job b = createJob("B", 10);

        b.waitFor(a);

        createGraph(Duration.ofMinutes(10), a, b).execute();

        StepExecutor.TimeContext timeContextA = a.getExecutor()
                                                 .getTimeContext();
        StepExecutor.TimeContext timeContextB = b.getExecutor()
                                                 .getTimeContext();

        long endTimeMillisA = timeContextA.getEndTimeMillis();
        long startTimeMillisB = timeContextB.getStartTimeMillis();

        assertThat(startTimeMillisB).isGreaterThanOrEqualTo(endTimeMillisA);
    }

    @Test
    void shouldWaitForLastJobFinished() {
        Job a = createJob("A", 200);

        createGraph(Duration.ofMinutes(10), a).execute();

        long endTimeMillisA = a.getExecutor()
                               .getTimeContext()
                               .getEndTimeMillis();
        assertThat(endTimeMillisA).isGreaterThan(0);
    }

    @Test
    void shouldTimeoutWhenJobExceeds() {
        long start = System.currentTimeMillis();

        Job a = createJob("A", 2000);

        createGraph(Duration.ofMillis(200), a).execute();

        long end = System.currentTimeMillis();

        assertThat(end - start).isBetween(150L, 350L);
    }

    private static ExecutionGraph createGraph(Duration timeout, Job... jobs) {
        ExecutionGraph.Builder builder = new ExecutionGraph.Builder().timeout(timeout);
        for (Job j : jobs) {
            builder.addJob(j);
        }
        ExecutionGraph graph = builder.build();

        graph.getJobs()
             .forEach(j -> j.getExecutor()
                            .getJobTrigger()
                            .registerHandler(graph));
        return graph;
    }

    private static Job createJob(String name, int delay) {
        StepExecutor executor = new StepExecutor("cmd " + name, Duration.ZERO, new JobTrigger(name)) {
            @Override
            public void runCommand() throws Exception {
                TimeUnit.MILLISECONDS.sleep(delay);
            }
        };

        return new Job(name, executor);
    }
}
