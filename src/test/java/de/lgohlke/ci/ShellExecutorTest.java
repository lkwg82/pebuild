package de.lgohlke.ci;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

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

        Result result = ShellExecutor.execute2(command);

        assertThat(result.getExitCode()).isEqualTo(2);
    }

    @Test
    void commandKilledWithTimeout() {
        String command = "sleep 1200";

        Result result = ShellExecutor.execute3(command, Duration.of(100, MILLIS));

        assertThat(result.getExitCode()).isEqualTo(137);
    }

    @Test
    void commandExitBeforeTimeout() {
        String command = "exit 3";

        Result result = ShellExecutor.execute3(command, Duration.of(100, MILLIS));

        assertThat(result.getExitCode()).isEqualTo(3);

    }

    private static class ShellExecutor {

        private static String execute(String command) throws IOException {
            ProcessBuilder processBuilder = createWrappedInShell(command);
            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            return builder.toString();
        }

        private static ProcessBuilder createWrappedInShell(String command) {
            String[] wrappedInShell = new String[]{"sh", "-c", command};

            return new ProcessBuilder(wrappedInShell);
        }

        @SneakyThrows
        private static Result execute2(String command) {
            Process process = createWrappedInShell(command).start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String stdout = builder.toString();
            return new Result(process.waitFor(), stdout);
        }

        @SneakyThrows
        private static Result execute3(String command, Duration maxDuration) {

            Process process = createWrappedInShell(command).start();
            String stdout = "";
            long seconds = maxDuration.getSeconds();
            int nano = maxDuration.getNano();
            long nanos_to_millis = TimeUnit.NANOSECONDS.toMillis(nano);
            long seconds_to_millis = TimeUnit.SECONDS.toMillis(seconds);

            long timeout = nanos_to_millis + seconds_to_millis;
            boolean exitedBeforeTimeout = process.waitFor(timeout, TimeUnit.MILLISECONDS);

            if (!exitedBeforeTimeout) {
                int exitCode = process.destroyForcibly()
                                      .waitFor();
                return new Result(exitCode, stdout);
            }
            return new Result(process.waitFor(), stdout);
        }
    }

    @RequiredArgsConstructor
    @Getter
    private static class Result {
        private final int exitCode;
        private final String stdout;
    }
}