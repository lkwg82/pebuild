package de.lgohlke.pebuild;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentConfigurerTest {
    @BeforeEach
    void setUp() {
        EnvironmentConfigurer.mergeEnvironmentAndSystemProperties();
    }

    @Test
    void shouldMergeEnvAndSystemProperties() {
        assertThat(System.getProperty("HOME")).isNotEmpty();
    }

    @Test
    void shouldPriotizeSystemProperties() {
        System.setProperty("HOME", "x");

        assertThat(System.getProperty("HOME")).isEqualTo("x");
    }

}
