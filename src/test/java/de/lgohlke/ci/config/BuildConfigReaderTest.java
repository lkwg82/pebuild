package de.lgohlke.ci.config;

import org.junit.jupiter.api.Test;

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
}
