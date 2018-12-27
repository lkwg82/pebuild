package de.lgohlke.ci.config.dto;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OptionsTest {
    private Options options = new Options();

    @Test
    void durationSetMinutes() {
        options.setTimeout("11m");

        assertThat(options.getTimeout()).isEqualTo(Duration.ofMinutes(11));
    }

    @Test
    void durationDefault() {
        assertThat(options.getTimeout()).isEqualTo(Duration.ofMinutes(10));
    }
}