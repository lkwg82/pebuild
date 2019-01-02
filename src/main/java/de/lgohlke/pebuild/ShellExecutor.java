package de.lgohlke.pebuild;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
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

    @Override
    public void runCommand() throws Exception {
        Process process = createWrappedInShell(getCommand()).start();

        String filename = "step." + getName() + ".output";

        if (Configuration.REPORT_DIRECTORY.value()
                                          .isEmpty()) {
            Configuration.REPORT_DIRECTORY.setIfMissing(System.getProperty("user.dir"));
        }

        Path outputFile = Paths.get(Configuration.REPORT_DIRECTORY.value(), filename);
        CombinedStreamFascade streamFascade = new CombinedStreamFascade(getName(),
                                                                        process.getInputStream(),
                                                                        process.getErrorStream(),
                                                                        outputFile);
        streamFascade.start();
        log.info("starting");
        process.waitFor();
        log.info("finished");
        TimeUnit.MILLISECONDS.sleep(50);
        streamFascade.stop();
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
            process.destroy();
            int exitCode = process.waitFor();
            return new ExecutionResult(exitCode, stdout);
        }
        return new ExecutionResult(process.waitFor(), stdout);
    }
}
