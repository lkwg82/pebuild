package de.lgohlke.pebuild.config.dto;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeoutOptionParserTest {
    private TimeoutOptionParser parser = new TimeoutOptionParser();

    @Test
    void durationSetMinutes() {
        assertThat(parser.parseString("11m")).isEqualTo(Duration.ofMinutes(11));
    }

    @Test
    void durationSetSeconds() {
        assertThat(parser.parseString("1m")).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void durationSetFail() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseString("11M"));
    }
}