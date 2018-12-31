package de.lgohlke.pebuild;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

        assertThat(output).contains("HOME=/home");
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

    @BeforeEach
    void setUp() throws IOException {
        Path path = Files.createTempDirectory(new Random().nextInt() + "");
        System.setProperty("user.dir",
                           path.toAbsolutePath()
                               .toString());
    }

    @Test
    void captureOutputAsFile() throws Exception {
        ShellExecutor shellExecutor = new ShellExecutor("test",
                                                        "echo hello err >&2; echo hello out",
                                                        Duration.ZERO,
                                                        new JobTrigger("test"));

        shellExecutor.runCommand();

        String content = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir"), "step.test.output")));
        assertThat(content).contains("hello out");
    }
}