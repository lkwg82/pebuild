package de.lgohlke.ci.config;

import de.lgohlke.ci.config.dto.BuildConfig;
import org.junit.jupiter.api.Test;

class BuildConfigReaderTest {
    private String yaml = "" +
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

    @Test
    void shouldParse() {
        BuildConfig buildConfig = BuildConfigReader.parse(yaml);

        System.out.println(buildConfig);
    }

}
