package de.lgohlke.pebuild;

import de.lgohlke.streamutils.MergingStreamFascade2;
import de.lgohlke.streamutils.PrefixedInputStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
class ShellExecutor extends StepExecutor {
    private Process process;

    ShellExecutor(String name, String command, Duration timeout) {
        super(name, command, timeout);
    }

    ShellExecutor(String name, String command) {
        this(name, command, Duration.ofDays(999));
    }

    @Override
    public ExecutionResult runCommand() throws Exception {
        process = startProcess();
        Path outputFile = prepareOutputFile();

        if (!process.isAlive()) {
            // TODO needs test
            StringWriter errOutput = new StringWriter();
            IOUtils.copy(process.getErrorStream(), errOutput, Charset.defaultCharset());
            log.error("failed to start: {}", errOutput.toString());
            val result = new ExecutionResult(process.exitValue());
            process = null;
            return result;
        }

        try (val fout = new FileOutputStream(outputFile.toFile())) {
            PrefixedInputStream stdout = new PrefixedInputStream(process.getInputStream(), "STDOUT");
            PrefixedInputStream stderr = new PrefixedInputStream(process.getErrorStream(), "STDERR");

            PrefixedInputStream[] inputStreams = {stdout, stderr};
            OutputStream[] outputStreams = {fout};

            MergingStreamFascade2.create(getName(), inputStreams, System.out, outputStreams);

            int exitCode = waitForProcess(process);
            log.debug("finished with exit code {}", exitCode);

            ExecutionResult executionResult = new ExecutionResult(exitCode);
            process = null;
            return executionResult;
        }
    }

    @Override
    public void cancel() {
        if (null == process) {
            log.debug("tried to cancel a not running instance");
            return;
        }
        process.destroy();
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
        ProcessBuilder processBuilder = prepareExecutionContext();

        log.debug("starting");
        Process process = processBuilder.start();

        if (process.isAlive()) {
            log.debug("raw input: {}", getCommand());
            String rawInput = getCommand() + "\n";
            byte[] cmd = rawInput.getBytes();

            try (val outputStream = process.getOutputStream()) {
                outputStream.write(cmd);
            }
        } else {
            log.warn("process already exited with code: {}", process.exitValue());
        }
        return process;

    }

    private ProcessBuilder prepareExecutionContext() {
        val osDetector = new OSDetector();
        if (osDetector.isLinux()) {
            // TODO handling with tini is very instable
//            val pathToTini = prepareTini();
//            String[] wrappedInShell = new String[]{pathToTini, "-s", "-vvvv", "-w", "-g", "sh"};
            String[] wrappedInShell = new String[]{"sh"};
            log.debug("raw command is '{}'", String.join(" ", wrappedInShell));
            return new ProcessBuilder(wrappedInShell);
        }

        if (osDetector.isMac()) {
            String[] wrappedInShell = new String[]{"sh"};
            log.debug("raw command is '{}'", String.join(" ", wrappedInShell));
            log.warn("" +
                             "dont have a reaper context, so the build " +
                             "can leave zombie processes behind " +
                             "(not yet implemented, see tini for linux)");
            return new ProcessBuilder(wrappedInShell);
        }

        log.error("your operating system is not supported (yet), consider a PR");
        System.exit(1);
        return null;
    }

    private String prepareTini() {
        val path = System.getenv("PATH");
        val tinitDownloader = new TiniDownloader(path);
        val found_path = tinitDownloader.tiniPath();
        if (found_path.getFirst()) {
            log.debug("has tini in PATH:{}", found_path.getSecond());
            return found_path.getSecond();
        } else {
            log.debug("download tini");
            // TODO too specific to maven
            val tiniPath = Paths.get("target", "bin");
            new File(tiniPath.toFile().getAbsolutePath()).mkdirs();
            Path pathToTini = tiniPath.resolve("tini");
            tinitDownloader.download(pathToTini);

            return pathToTini.toFile().getAbsolutePath();
        }
    }

    private static class OSDetector {
        private OSDetector() {
        }

        private static final String OS = System.getProperty("os.name").toLowerCase();

        boolean isMac() {
            return OS.contains("mac");
        }

        boolean isLinux() {
            return OS.contains("linux");
        }
    }
}
