package de.lgohlke.pebuild;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

class ShellExecutorTest {
    @Test
    void executeInShell() throws IOException {
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

        assertThat(result.getExitCode()).isEqualTo(137);
    }

    @Test
    void commandExitBeforeTimeout() {
        String command = "exit 3";

        ExecutionResult result = ShellExecutor.execute3(command, Duration.of(100, MILLIS));

        assertThat(result.getExitCode()).isEqualTo(3);

    }

}