package de.lgohlke.pebuild.graph.validators;

import de.lgohlke.pebuild.StepExecutor;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferencedJobMissingValidatorTest {

    private StepExecutor a = new StepExecutor("a", "A") {
    };
    private StepExecutor b = new StepExecutor("b", "B") {
    };

    @Test
    void detectMissingReferencedJob() {
        b.waitFor(a);

        List<StepExecutor> jobs = Lists.newArrayList(b);

        assertThrows(ReferencedJobMissingValidator.ReferencedJobsMissing.class,
                     () -> ReferencedJobMissingValidator.validate(jobs));
    }

    @Test
    void shouldNotDetectAnyMissingReferencedJob() {
        b.waitFor(a);

        List<StepExecutor> jobs = Lists.newArrayList(a, b);

        ReferencedJobMissingValidator.validate(jobs);
    }
}