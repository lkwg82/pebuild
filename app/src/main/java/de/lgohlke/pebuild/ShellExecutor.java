package de.lgohlke.pebuild;

import de.lgohlke.streamutils.MergingStreamFascade;
import de.lgohlke.streamutils.PrefixedInputStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
class ShellExecutor extends StepExecutor {
    private final boolean syncAfter;

    ShellExecutor(String name, String command, Duration timeout, JobTrigger jobTrigger) {
        this(name, command, timeout, jobTrigger, false);
    }

    ShellExecutor(String name, String command, Duration timeout, JobTrigger jobTrigger, boolean syncAfter) {
        super(name, command, timeout, jobTrigger);
        this.syncAfter = syncAfter;
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

        System.getProperties()
              .forEach((k, v) -> {
                  if (k.toString()
                       .contains("log")) {
                      log.error("key {} -> {}", k, v);
                  }
              });

        log.info("executing: '{}'", getCommand());
        ProcessBuilder processBuilder = createWrappedInShell(getCommand());

        log.debug("starting");
        Process process = processBuilder.start();

        String filename = "step." + getName() + ".output";

        if (Configuration.REPORT_DIRECTORY.value()
                                          .isEmpty()) {
            Configuration.REPORT_DIRECTORY.setIfMissing(System.getProperty("user.dir"));
        }

        val outputFile = Paths.get(Configuration.REPORT_DIRECTORY.value(), filename);
        try (val fout = new FileOutputStream(outputFile.toFile())) {
            PrefixedInputStream stdout = new PrefixedInputStream(process.getInputStream(), "STDOUT");
            PrefixedInputStream stderr = new PrefixedInputStream(process.getErrorStream(), "STDERR");

            PrefixedInputStream[] inputStreams = {stdout, stderr};
            OutputStream[] outputStreams = {fout};

            try (val ignored = MergingStreamFascade.create(getName(), inputStreams, System.out, outputStreams)) {
                process.waitFor();
                log.debug("finished");
            }

            // this is only for tests when immediately the output needs to be verified
            if (syncAfter) {
                fout.flush();
                fout.getFD()
                    .sync();
            }
        }
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
