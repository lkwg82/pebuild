package de.lgohlke.pebuild.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BuildConfigReaderTest {

    @Test
    void shouldParse() {
        String yaml = "" +
                "options: \n" +
                "  timeout: 10m\n" +
                "\n" +
                "steps:\n" +
                "- name: demo\n" +
                "  command: 'date'\n" +
                "- name: sleep\n" +
                "  command: 'sleep 2'\n" +
                "  timeout: 10s\n" +
                "  waitfor: ['demo']";

        BuildConfigReader.parse(yaml);
    }

    @Nested
    class errors {
        @Test
        void failOnMissingSteps_implicite() {
            assertThrows(BuildConfigReader.MissingSteps.class, () -> BuildConfigReader.parse("options:"));
        }

        @Test
        void failOnMissingSteps_explicite() {
            assertThrows(BuildConfigReader.MissingSteps.class, () -> BuildConfigReader.parse("steps:"));
        }

        @Test
        void failOnEmptyConfig() {
            assertThrows(BuildConfigReader.EmptyConfig.class, () -> BuildConfigReader.parse(""));
        }

        @Test
        void failOnWrongConfig() {
            assertThrows(MarkedYAMLException.class, () -> BuildConfigReader.parse("x"));
        }

        @Test
        void failOnMissingCommandInStep() {
            String config = "" +
                    "steps:\n" +
                    " - name: test\n";

            Executable executable = () -> BuildConfigReader.parse(config);

            assertThrows(BuildConfigReader.MissingCommandInStepException.class, executable);
        }
    }
}
