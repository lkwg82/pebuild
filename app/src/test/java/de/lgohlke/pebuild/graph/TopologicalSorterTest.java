package de.lgohlke.pebuild.graph;

import de.lgohlke.pebuild.StepExecutor;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TopologicalSorterTest {
    @Test
    void shouldBeSorted() {

        StepExecutor a = create("A");
        StepExecutor b = create("B");
        StepExecutor c = create("C");

        b.waitFor(a);
        c.waitFor(a);
        c.waitFor(b);

        List<StepExecutor> jobs = Lists.newArrayList(a, b, c);
        Collection<StepExecutor> sortedJobs = TopologicalSorter.sort(jobs);

        assertThat(sortedJobs).startsWith(a);
        assertThat(sortedJobs).containsSequence(b, c);
    }

    private StepExecutor create(String name) {
        return new StepExecutor(name, name, Duration.ZERO) {
        };
    }
}