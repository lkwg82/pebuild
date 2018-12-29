package de.lgohlke.pebuild;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationTest {
    @BeforeEach
    void setUp() {
        System.clearProperty("PEBUILD_FILE");
    }

    @Test
    void setIfMissing() {
        assertThat(System.getProperties()).doesNotContainKey("PEBUILD_FILE");

        Configuration.configureDefaults();

        assertThat(System.getProperties()).containsKey("PEBUILD_FILE");
    }

    @Test
    void dontOverwriteWhenExisting() {
        System.setProperty("PEBUILD_FILE", "x");

        Configuration.configureDefaults();

        assertThat(System.getProperty("PEBUILD_FILE")).isEqualTo("x");
    }
}