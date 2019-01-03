package de.lgohlke.pebuild.graph.validators;

import de.lgohlke.pebuild.JobTrigger;
import de.lgohlke.pebuild.StepExecutor;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CycleValidatorTest {
    private StepExecutor a = new StepExecutor("a", "A", Duration.ZERO, new JobTrigger("a")) {
    };
    private StepExecutor b = new StepExecutor("b", "B", Duration.ZERO, new JobTrigger("b")) {
    };

    @Test
    void detectCycles() {
        b.waitFor(a);
        a.waitFor(b);

        List<StepExecutor> jobs = Lists.newArrayList(a, b);

        assertThrows(CycleValidator.CycleDetected.class, () -> CycleValidator.validate(jobs));
    }

    @Test
    void detectSelfCycles() {
        b.waitFor(b);

        List<StepExecutor> jobs = Lists.newArrayList(b);

        assertThrows(CycleValidator.CycleDetected.class, () -> CycleValidator.validate(jobs));
    }

    @Test
    void detectNoCycles() {
        b.waitFor(a);

        List<StepExecutor> jobs = Lists.newArrayList(a, b);

        CycleValidator.validate(jobs);
    }
}