package de.lgohlke.pebuild;

import org.junit.jupiter.api.Test;

import java.time.Duration;

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
}