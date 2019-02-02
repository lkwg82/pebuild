package de.lgohlke.pebuild;

import lombok.val;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.scheduler.Schedulers.elastic;

class ShellExecutorIT {

    private static final Duration WAIT_MAX_500_MS = Duration.ofMillis(500);

    static {
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.Channel", "DEBUG");
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.MergingStreamFascade", "DEBUG");
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.DecoratingStreamer", "DEBUG");

        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.pebuild.ShellExecutor", "DEBUG");
    }

    private Path tempDirectory;

    @BeforeEach
    void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory(new Random().nextInt() + "");
        Configuration.REPORT_DIRECTORY.overwrite(tempDirectory.toAbsolutePath()
                                                              .toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    @RepeatedTest(50)
    void captureOutputAsFile() throws Exception {
        val random = new SecureRandom().nextInt();
        val command = "echo hello err >&2; echo hello out " + random;
        val shellExecutor = new ShellExecutor("test", command);

        shellExecutor.runCommand();

        val output = Paths.get(Configuration.REPORT_DIRECTORY.value(), "step.test.output");
        val content = new String(Files.readAllBytes(output));
        assertThat(content).contains("hello out " + random);
    }

    @Test
    void shouldLazyCreateReportDirectoryIfMissing() throws Exception {
        Configuration.REPORT_DIRECTORY.overwrite(tempDirectory.toAbsolutePath()
                                                              .toString() + "/x/s");

        val shellExecutor = new ShellExecutor("test", "env");

        shellExecutor.runCommand();
    }

    @Nested
    class exitCodes {

        @Test
        void shouldPropagateExitCodeOnFailedCommand() {
            val shellExecutor = new ShellExecutor("test", "exit 23");

            val mono = Mono.fromCallable(shellExecutor::runCommand);

            StepVerifier.create(mono)
                        .expectNext(new ExecutionResult(23))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }

        @Test
        void shouldPropagateExitCodeOnWrongCommand() {
            val shellExecutor = new ShellExecutor("test", "kjakdhaksdhk");

            val mono = Mono.fromCallable(shellExecutor::runCommand);

            StepVerifier.create(mono)
                        .expectNext(new ExecutionResult(127))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }
    }

    @Nested
    class timeout {
        @Test
        void exitsBeforeTimeout() {
            val shellExecutor = new ShellExecutor("test", "exit 3", Duration.ofSeconds(1));

            val mono = Mono.fromCallable(shellExecutor::runCommand);

            StepVerifier.create(mono)
                        .expectNext(new ExecutionResult(3))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }

        @Test
        void exitsWithTimeoutButProcessIsKindOfBlocking() {
            val shellExecutor = new ShellExecutor("test", "sleep 777", Duration.ofMillis(100));

            val mono = Mono.fromCallable(shellExecutor::runCommand);

            StepVerifier.create(mono)
                        .expectNext(new ExecutionResult(128 + 15))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }
    }

    @Nested
    class cancelation {
        private ShellExecutor shellExecutor;

        @BeforeEach
        void setUp() {
            shellExecutor = new ShellExecutor("test", "sleep 20");
        }

        @Test
        void shouldCancel() {

            val mono = getMono(shellExecutor);

            StepVerifier.create(mono)
                        .then(wait100ms())
                        .then(shellExecutor::cancel)
                        .expectNext(new ExecutionResult(143))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }

        @NotNull
        private Runnable wait100ms() {
            return () -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        }

        private Mono<ExecutionResult> getMono(ShellExecutor shellExecutor) {
            return Mono.fromCallable(shellExecutor::runCommand).subscribeOn(elastic());
        }

        @Test
        void shouldNotFailOnCancelNotRunningProcess() {
            shellExecutor.cancel();
        }

        @Test
        void shouldNotFailCancelTwice() {

            val mono = getMono(shellExecutor);

            StepVerifier.create(mono)
                        .then(wait100ms())
                        .then(shellExecutor::cancel)
                        .then(shellExecutor::cancel)
                        .expectNext(new ExecutionResult(143))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }
    }
}