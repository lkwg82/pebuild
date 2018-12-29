package de.lgohlke.pebuild.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JobTest {
    @Test
    void failOnDuplicateWaitForJob() {
        Job a = new Job("A");
        Job b = new Job("B");

        a.waitFor(b);

        assertThrows(IllegalArgumentException.class, () -> a.waitFor(b));
    }
}