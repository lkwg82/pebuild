package de.lgohlke.pebuild.graph.validators;

import de.lgohlke.pebuild.JobTrigger;
import de.lgohlke.pebuild.StepExecutor;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferencedJobMissingValidatorTest {

    private StepExecutor a = new StepExecutor("a", "A", Duration.ZERO, new JobTrigger("a")) {
    };
    private StepExecutor b = new StepExecutor("b", "B", Duration.ZERO, new JobTrigger("b")) {
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