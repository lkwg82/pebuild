package de.lgohlke.pebuild;

import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;
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
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.DecoratingStreamer2", "DEBUG");

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

            StepVerifier.create(shellExecutor.getResults())
                        .then(() -> execute(shellExecutor))
                        .expectNext(new ExecutionResult(23))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }

        @Test
        void shouldPropagateExitCodeOnWrongCommand() {
            val shellExecutor = new ShellExecutor("test", "kjakdhaksdhk");

            StepVerifier.create(shellExecutor.getResults())
                        .then(() -> execute(shellExecutor))
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

            StepVerifier.create(shellExecutor.getResults())
                        .then(() -> execute(shellExecutor))
                        .expectNext(new ExecutionResult(3))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }

        @Test
        void exitsWithTimeoutButProcessIsKindOfBlocking() {
            val shellExecutor = new ShellExecutor("test", "sleep 777", Duration.ofMillis(100));

            StepVerifier.create(shellExecutor.getResults())
                        .then(() -> execute(shellExecutor))
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
            StepVerifier.create(shellExecutor.getResults())
                        .then(() -> execute(shellExecutor))
                        .then(shellExecutor::cancel)
                        .expectNext(new ExecutionResult(143))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }

        @Test
        void shouldNotFailOnCancelNotRunningProcess() {
            shellExecutor.cancel();
        }

        @Test
        void shouldNotFailCancelTwice() {
            StepVerifier.create(shellExecutor.getResults())
                        .then(() -> execute(shellExecutor))
                        .then(shellExecutor::cancel)
                        .then(shellExecutor::cancel)
                        .expectNext(new ExecutionResult(143))
                        .expectComplete()
                        .verify(WAIT_MAX_500_MS);
        }
    }

    @SneakyThrows
    private void execute(ShellExecutor shellExecutor) {
        Flux.defer(() -> execution(shellExecutor).subscribeOn(elastic()))
            .subscribe();
        TimeUnit.MILLISECONDS.sleep((long) 150);
    }

    private Mono<Object> execution(ShellExecutor shellExecutor) {
        return Mono.fromRunnable(() -> {
            try {
                shellExecutor.runCommand();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}