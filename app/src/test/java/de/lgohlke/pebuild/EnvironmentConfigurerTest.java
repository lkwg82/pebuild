package de.lgohlke.pebuild;

import lombok.val;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    @Nested
    class MeaningfulDefaults {

        private EnvironmentConfigurer environmentConfigurer;
        private String workingDirectory;

        @BeforeEach
        void setUp() throws IOException {
            val directory = Files.createTempDirectory("aasdasd");
            workingDirectory = directory.toFile()
                                        .getCanonicalPath();
            environmentConfigurer = new EnvironmentConfigurer(workingDirectory);
        }

        @AfterEach
        void tearDown() throws IOException {
            FileUtils.deleteDirectory(new File(workingDirectory));
        }

        @Test
        void configureForMaven() throws IOException {
            Files.write(Paths.get(workingDirectory, "pom.xml"), "test".getBytes());

            environmentConfigurer.configureMeaningfullDefaults();

            assertThat(Configuration.REPORT_DIRECTORY.value()).isEqualTo("target/pebuild.d");
        }
    }
}
