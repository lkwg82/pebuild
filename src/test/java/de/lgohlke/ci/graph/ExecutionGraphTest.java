package de.lgohlke.ci.graph;

import org.junit.jupiter.api.Test;

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
}
