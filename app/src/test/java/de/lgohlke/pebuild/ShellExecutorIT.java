package de.lgohlke.pebuild;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static java.time.Duration.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

class ShellExecutorIT {

    static {
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.pebuild.Channel", "DEBUG");
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.pebuild.MergingStreamFascade", "DEBUG");
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

    @RepeatedTest(100)
    void captureOutputAsFile() throws Exception {
        String command = "echo hello err >&2; echo hello out";
        ShellExecutor shellExecutor = new ShellExecutor("test", command, ZERO, new JobTrigger("test"), true);

        shellExecutor.runCommand();

        Path output = Paths.get(Configuration.REPORT_DIRECTORY.value(), "step.test.output");
        String content = new String(Files.readAllBytes(output));
        assertThat(content).contains("hello out");
    }

    @Test
    void shouldLazyCreateReportDirectoryIfMissing() throws Exception {
        Configuration.REPORT_DIRECTORY.overwrite(tempDirectory.toAbsolutePath()
                                                              .toString() + "/x/s");

        ShellExecutor shellExecutor = new ShellExecutor("test", "env", ZERO, new JobTrigger("test"));

        shellExecutor.runCommand();
    }
}