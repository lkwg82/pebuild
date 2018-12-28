package de.lgohlke.ci.graph.validators;

import de.lgohlke.ci.graph.Job;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CycleValidatorTest {
    private Job a = new Job("A");
    private Job b = new Job("B");

    @Test
    void detectCycles() {
        b.waitFor(a);
        a.waitFor(b);

        List<Job> jobs = Lists.newArrayList(a, b);

        assertThrows(CycleValidator.CycleDetected.class, () -> CycleValidator.validate(jobs));
    }

    @Test
    void detectSelfCycles() {
        b.waitFor(b);

        List<Job> jobs = Lists.newArrayList(b);

        assertThrows(CycleValidator.CycleDetected.class, () -> CycleValidator.validate(jobs));
    }

    @Test
    void detectNoCycles() {
        b.waitFor(a);

        List<Job> jobs = Lists.newArrayList(a, b);

        CycleValidator.validate(jobs);
    }
}