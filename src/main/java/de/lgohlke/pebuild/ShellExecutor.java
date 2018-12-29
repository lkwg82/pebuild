package de.lgohlke.pebuild;

import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

class ShellExecutor extends StepExecutor {
    ShellExecutor(String name, String command, Duration timeout, JobTrigger jobTrigger) {
        super(name, command, timeout, jobTrigger);
    }

    @SneakyThrows
    static void executeInheritedIO(String command) {
        ProcessBuilder processBuilder = createWrappedInShell(command);

        processBuilder.inheritIO()
                      .start()
                      .waitFor();
    }

    @SneakyThrows
    static String execute(String command) {
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
    static ExecutionResult execute2(String command) {
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
        return new ExecutionResult(process.waitFor(), stdout);
    }

    @SneakyThrows
    static ExecutionResult execute3(String command, Duration maxDuration) {

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
            return new ExecutionResult(exitCode, stdout);
        }
        return new ExecutionResult(process.waitFor(), stdout);
    }
}
