package de.lgohlke.pebuild.graph.validators;

import de.lgohlke.pebuild.StepExecutor;
import lombok.val;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CycleValidatorTest {
    private StepExecutor a = new StepExecutor("a", "A") {
    };
    private StepExecutor b = new StepExecutor("b", "B") {
    };

    @Test
    void detectCycles() {
        b.waitFor(a);
        a.waitFor(b);

        val jobs = Lists.newArrayList(a, b);

        assertThrows(CycleValidator.CycleDetected.class, () -> CycleValidator.validate(jobs));
    }

    @Test
    void detectSelfCycles() {
        b.waitFor(b);

        val jobs = Lists.newArrayList(b);

        assertThrows(CycleValidator.CycleDetected.class, () -> CycleValidator.validate(jobs));
    }

    @Test
    void detectNoCycles() {
        b.waitFor(a);

        val jobs = Lists.newArrayList(a, b);

        CycleValidator.validate(jobs);
    }
}