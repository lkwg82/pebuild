package de.lgohlke.pebuild;

import lombok.val;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Random;

import static java.time.Duration.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

class ShellExecutorIT {

    static {
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.Channel", "DEBUG");
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.MergingStreamFascade", "DEBUG");
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.DecoratingStreamer", "DEBUG");

        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.pebuild.ShellExecutor", "DEBUG");
    }

    private Path tempDirectory;

    @BeforeEach
    void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory(new Random().nextInt() + "");
        Configuration.REPORT_DIRECTORY.overwrite(tempDirectory.toAbsolutePath()
                                                              .toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    private JobTrigger trigger = new JobTrigger("test");

    @Test
    void captureOutputAsFile() throws Exception {
        val command = "echo hello err >&2; echo hello out";
        val shellExecutor = new ShellExecutor("test", command, ZERO, trigger, true);

        shellExecutor.runCommand();

        val output = Paths.get(Configuration.REPORT_DIRECTORY.value(), "step.test.output");
        val content = new String(Files.readAllBytes(output));
        assertThat(content).contains("hello out");
    }

    @Test
    void shouldLazyCreateReportDirectoryIfMissing() throws Exception {
        Configuration.REPORT_DIRECTORY.overwrite(tempDirectory.toAbsolutePath()
                                                              .toString() + "/x/s");

        val shellExecutor = new ShellExecutor("test", "env", ZERO, trigger);

        shellExecutor.runCommand();
    }

    @Test
    void shouldPropagateExitCode() throws Exception {
        val shellExecutor = new ShellExecutor("test", "exit 23", ZERO, trigger);

        val result = shellExecutor.runCommand();

        assertThat(result.getExitCode()).isEqualTo(23);
    }

    @Nested
    class timeout {
        @Test
        void exitsBeforeTimeout() throws Exception {
            val shellExecutor = new ShellExecutor("test", "exit 3", Duration.ofSeconds(1), trigger);

            val result = shellExecutor.runCommand();

            assertThat(result.getExitCode()).isEqualTo(3);
        }

        @Test
        void exitsWithTimeoutButProcessIsKindOfBlocking() throws Exception {
            val shellExecutor = new ShellExecutor("test", "exec sleep 777", Duration.ofSeconds(1), trigger);

            val result = shellExecutor.runCommand();

            assertThat(result.getExitCode()).isEqualTo(128 + 15);
        }

        @Test
        void exitsWithTimeout() throws Exception {
            val shellExecutor = new ShellExecutor("test", "read x", Duration.ofSeconds(1), trigger);

            val result = shellExecutor.runCommand();

            assertThat(result.getExitCode()).isEqualTo(128 + 15);
        }
    }
}