package de.lgohlke.pebuild.graph.validators;

import de.lgohlke.pebuild.graph.Job;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferencedJobMissingValidatorTest {

    private Job a = new Job("A");
    private Job b = new Job("B");

    @Test
    void detectMissingReferencedJob() {
        b.waitFor(a);

        List<Job> jobs = Lists.newArrayList(b);

        assertThrows(ReferencedJobMissingValidator.ReferencedJobsMissing.class,
                     () -> ReferencedJobMissingValidator.validate(jobs));
    }

    @Test
    void shouldNotDetectAnyMissingReferencedJob() {
        b.waitFor(a);

        List<Job> jobs = Lists.newArrayList(a, b);

        ReferencedJobMissingValidator.validate(jobs);
    }
}