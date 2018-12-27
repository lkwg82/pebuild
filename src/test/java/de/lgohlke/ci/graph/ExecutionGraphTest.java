package de.lgohlke.ci.graph;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionGraphTest {
    @Test
    void printGraph() {

        Job a = new Job("A");
        Job b = new Job("B");
        Job c = new Job("C");

        b.waitFor(a);
        c.waitFor(a);
        c.waitFor(b);

        ExecutionGraph graph = new ExecutionGraph.Builder().addJob(c)
                                                           .addJob(b)
                                                           .addJob(a)
                                                           .build();

        assertThat(graph.toString()).isEqualTo("(A [])->(B [A])->(C [A,B])");
    }

    @Nested
    class validate {
        @Test
        void detectCycles() {

            Job a = new Job("A");
            Job b = new Job("B");

            b.waitFor(a);
            a.waitFor(b);

            ExecutionGraph.Builder builder = new ExecutionGraph.Builder().addJob(b)
                                                                         .addJob(a);

            assertThrows(ExecutionGraph.Builder.CycleDetected.class, builder::validate);
        }

        @Test
        void detectNoCycles() {

            Job a = new Job("A");
            Job b = new Job("B");

            b.waitFor(a);


            ExecutionGraph.Builder builder = new ExecutionGraph.Builder().addJob(b)
                                                                         .addJob(a);

            builder.validate();
        }

        @Test
        void detectMissingReferencedJob() {

            Job a = new Job("A");
            Job b = new Job("B");

            b.waitFor(a);


            ExecutionGraph.Builder builder = new ExecutionGraph.Builder().addJob(b);

            assertThrows(ExecutionGraph.Builder.ReferencedJobsMissing.class, builder::validate);
        }
    }


}
