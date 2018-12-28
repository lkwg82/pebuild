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

        ExecutionGraph graph = new ExecutionGraph.Builder().addJob(a)
                                                           .addJob(b)
                                                           .build();

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
        StepExecutor executorA = new StepExecutor("A", Duration.ZERO, new JobTrigger("A")) {
            @Override
            public void runCommand() {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        StepExecutor executorB = new StepExecutor("B", Duration.ZERO, new JobTrigger("B")) {
            @Override
            public void runCommand() {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Job a = new Job("A", executorA);
        Job b = new Job("B", executorB);

        b.waitFor(a);

        ExecutionGraph graph = new ExecutionGraph.Builder().addJob(a)
                                                           .addJob(b)
                                                           .build();

        graph.getJobs()
             .forEach(j -> j.getExecutor()
                            .getJobTrigger()
                            .registerHandler(graph));

        // action
        graph.execute();


        StepExecutor.TimeContext timeContextA = executorA.getTimeContext();
        StepExecutor.TimeContext timeContextB = executorB.getTimeContext();

        long endTimeMillisA = timeContextA.getEndTimeMillis();
        long startTimeMillisB = timeContextB.getStartTimeMillis();

        assertThat(startTimeMillisB).isGreaterThanOrEqualTo(endTimeMillisA);
    }

    @Test
    void shouldWaitForLastJobFinished() {
        StepExecutor executorA = new StepExecutor("A", Duration.ZERO, new JobTrigger("A")) {
            @Override
            public void runCommand() {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Job a = new Job("A", executorA);

        ExecutionGraph graph = new ExecutionGraph.Builder().addJob(a)
                                                           .build();

        graph.getJobs()
             .forEach(j -> j.getExecutor()
                            .getJobTrigger()
                            .registerHandler(graph));

        // action
        graph.execute();

        long endTimeMillisA = executorA.getTimeContext()
                                       .getEndTimeMillis();
        assertThat(endTimeMillisA).isGreaterThan(0);
    }
}
