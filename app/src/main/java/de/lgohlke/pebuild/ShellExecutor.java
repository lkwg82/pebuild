package de.lgohlke.pebuild;

import de.lgohlke.streamutils.MergingStreamFascade;
import de.lgohlke.streamutils.PrefixedInputStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
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

    @Override
    public ExecutionResult runCommand() throws Exception {
        Process process = startProcess();
        Path outputFile = prepareOutputFile();

        try (val fout = new FileOutputStream(outputFile.toFile())) {
            PrefixedInputStream stdout = new PrefixedInputStream(process.getInputStream(), "STDOUT");
            PrefixedInputStream stderr = new PrefixedInputStream(process.getErrorStream(), "STDERR");

            PrefixedInputStream[] inputStreams = {stdout, stderr};
            OutputStream[] outputStreams = {fout};

            try (val streamFascade = MergingStreamFascade.create(getName(), inputStreams, System.out, outputStreams)) {
                int exitCode = waitForProcess(process);
                log.debug("finished with exit code {}", exitCode);

                if (streamFascade.isStreaming()) {
                    streamFascade.cancel();
                    log.warn("blocking sub process: '{}'", getCommand());
                }

                return new ExecutionResult(exitCode, "");
            } finally {
                // this is only for tests when immediately the output needs to be verified
                if (syncAfter) {
                    fout.flush();
                    fout.getFD()
                        .sync();
                }
            }
        }
    }

    private int waitForProcess(Process process) throws InterruptedException {
        @NonNull Duration timeout = getTimeout();
        if (timeout.isZero()) {
            return process.waitFor();
        } else {
            if (process.waitFor(timeout.getSeconds(), TimeUnit.SECONDS)) {
                log.debug("exits before timeout");
                return process.exitValue();
            } else {
                log.debug("timout");
                process.destroy();
                if (process.waitFor(1, TimeUnit.SECONDS)) {
                    log.debug("terminated after timeout");
                    return process.exitValue();
                } else {
                    log.warn("failed to terminate");
                    process.destroyForcibly();
                    log.warn("killed");

                    return process.exitValue();
                }
            }
        }
    }

    @NotNull
    private Path prepareOutputFile() {
        String filename = "step." + getName() + ".output";

        if (Configuration.REPORT_DIRECTORY.value()
                                          .isEmpty()) {
            Configuration.REPORT_DIRECTORY.setIfMissing(System.getProperty("user.dir"));
        }

        val outputFile = Paths.get(Configuration.REPORT_DIRECTORY.value(), filename);
        val directory = outputFile.getParent()
                                  .toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return outputFile;
    }

    @NotNull
    private Process startProcess() throws IOException {
        log.info("executing: '{}'", getCommand());
        ProcessBuilder processBuilder = startTinyInitWithShellAsRoot();

        log.debug("starting");
        Process process = processBuilder.start();

        log.debug("raw input: {}", getCommand());
        String rawInput = getCommand() + "\n";
        byte[] cmd = rawInput.getBytes();

        try (val outputStream = process.getOutputStream()) {
            outputStream.write(cmd);
        }
        return process;
    }

    private static ProcessBuilder startTinyInitWithShellAsRoot() {

        // TODO put tini into the path

        String[] wrappedInShell = new String[]{"/home/lars/Downloads/tini", "-s", "-vvvv", "-w", "-g", "sh"};
        log.debug("raw command is '{}'", String.join(" ", wrappedInShell));
        return new ProcessBuilder(wrappedInShell);
    }
}
