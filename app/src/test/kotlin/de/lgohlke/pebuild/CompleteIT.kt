package de.lgohlke.pebuild

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit

class CompleteIT {
    @BeforeEach
    fun setUp() {
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.Channel", "DEBUG")
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.MergingStreamFascade", "DEBUG")
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.DecoratingStreamer", "DEBUG")
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.pebuild.ShellExecutor", "DEBUG")
    }

    @RepeatedTest(100)
    fun simpleRun() {
        val targetPath = Paths.get("target", "pebuild.d_" + System.currentTimeMillis())
        val absolutePath = targetPath.toFile().absolutePath

        Configuration.REPORT_DIRECTORY.overwrite(absolutePath)
        System.setProperty("PEBUILD_FILE", "src/test/resources/integration/simple.pbuild.yml")

        // action
        Main().run()

        assertThat(targetPath).isDirectory()
        // TODO how should the timings being captured
        // assertThat(Paths.get("target", "pebuild.d", "timings")).isRegularFile()
        val outputPath = Paths.get(absolutePath, "step.second.output")
        assertThat(outputPath).isRegularFile()
        retryWithDelay(20, Duration.ofMillis(100), Runnable { assertThat(outputPath).hasContent("STDOUT hello world") })
    }

    private fun retryWithDelay(retry: Int,
                               delay: Duration,
                               check: Runnable) {

        var counter = 0

        while (counter++ < retry) {
            try {
                check.run()
            } catch (e: AssertionError) {
                if (counter >= retry) {
                    throw e
                }
                System.err.println("RETRY $counter")
                TimeUnit.MILLISECONDS.sleep(delay.toMillis())
            }
        }
    }

    @Test
    fun failOnMissingConfigFile() {
        System.setProperty("PEBUILD_FILE", "unknown.pbuild.yml")

        try {
            Main().run()

            fail<Any>("should fail on missing config")
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("missing config file: unknown.pbuild.yml")
        }
    }

    // TODO hangs on Linux, when shellexecutor init fails to start

    @Test
    internal fun `should exec date`() {
        Configuration.REPORT_DIRECTORY.overwrite("target/pebuild.d")
        Main.fromJava(arrayOf("-d", "exec", "-e", "date"))
    }

    @Test
    internal fun `should show help`() {
        Main.fromJava(arrayOf("-h"))
    }

    @Test
    internal fun `should exec exit 1`() {
        Configuration.REPORT_DIRECTORY.overwrite("target/pebuild.d")
        Main.fromJava(arrayOf("exec", "-e", "exit 1"))
    }
}