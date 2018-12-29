package de.lgohlke.pebuild.graph;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TopologicalSorterTest {
    @Test
    void shouldBeSorted() {

        Job a = new Job("A");
        Job b = new Job("B");
        Job c = new Job("C");

        b.waitFor(a);
        c.waitFor(a);
        c.waitFor(b);

        List<Job> jobs = Lists.newArrayList(a, b, c);
        Collection<Job> sortedJobs = TopologicalSorter.sort(jobs);

        assertThat(sortedJobs).startsWith(a);
        assertThat(sortedJobs).containsSequence(b, c);
    }
}