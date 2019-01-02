package de.lgohlke.pebuild;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Random;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

class ShellExecutorTest {


    @Test
    void executeInShell() {
        String command = "env";

        String output = ShellExecutor.execute(command);

        assertThat(output).contains("HOME=/");
    }

    @Test
    void captureExitCode() {
        String command = "exit 2";

        ExecutionResult result = ShellExecutor.execute2(command);

        assertThat(result.getExitCode()).isEqualTo(2);
    }

    @Test
    void commandKilledWithTimeout() {
        String command = "sleep 1200";

        ExecutionResult result = ShellExecutor.execute3(command, Duration.of(100, MILLIS));

        assertThat(result.getExitCode()).isEqualTo(143);
    }

    @Test
    void commandExitBeforeTimeout() {
        String command = "exit 3";

        ExecutionResult result = ShellExecutor.execute3(command, Duration.of(100, MILLIS));

        assertThat(result.getExitCode()).isEqualTo(3);
    }

    static {
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.pebuild.Channel", "DEBUG");
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.pebuild.CombinedStreamFascade", "DEBUG");
    }

    @Test
    @RepeatedTest(100)
    void captureOutputAsFile() throws Exception {
        Path path = Files.createTempDirectory(new Random().nextInt() + "");
        Configuration.REPORT_DIRECTORY.overwrite(path.toAbsolutePath()
                                                     .toString());
        try {
            ShellExecutor shellExecutor = new ShellExecutor("test",
                                                            "echo hello err >&2; echo hello out",
                                                            Duration.ZERO,
                                                            new JobTrigger("test"));

            shellExecutor.runCommand();

            Path output = Paths.get(Configuration.REPORT_DIRECTORY.value(), "step.test.output");
            String content = new String(Files.readAllBytes(output));
            assertThat(content).contains("hello out");
        } finally {
            FileUtils.deleteDirectory(path.toFile());
        }
    }
}