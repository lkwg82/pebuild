package de.lgohlke.pebuild;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class CompleteIT {
    @Test
    void simpleRun() {
        System.setProperty("PEBUILD_FILE", "src/test/resources/integration/simple.pbuild.yml");

        new Main().run();

        assertThat(Paths.get("target", "pebuild.d")).isDirectory();
        assertThat(Paths.get("target", "pebuild.d", "timings")).isRegularFile();
    }

    @Test
    void failOnMissingConfigFile() {
        System.setProperty("PEBUILD_FILE", "unknown.pbuild.yml");

        try {
            new Main().run();

            fail("should fail on missing config");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("missing config file: unknown.pbuild.yml");
        }
    }
}
