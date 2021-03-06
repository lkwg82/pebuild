package de.lgohlke.pebuild

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import picocli.CommandLine
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Paths

class MainIT {
    private val out = ByteArrayOutputStream()
    private val err = ByteArrayOutputStream()

    private val outP = PrintStream(out)
    private val errP = PrintStream(err)

    @BeforeEach
    fun setUp() {
        System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke", "DEBUG")
    }

    @RepeatedTest(10)
    fun simpleRun() {
        val targetPath = Paths.get("target", "pebuild.d_" + System.currentTimeMillis())
        val absolutePath = targetPath.toFile().absolutePath

        Configuration.REPORT_DIRECTORY.overwrite(absolutePath)
        System.setProperty("PEBUILD_FILE", "src/test/resources/integration/simple.pbuild.yml")

        // action
        Main.fromJava(arrayOf("run"), System.out, System.err)

        assertThat(targetPath).isDirectory()
        // TODO how should the timings being captured
        // assertThat(Paths.get("target", "pebuild.d", "timings")).isRegularFile()
        val outputPath = Paths.get(absolutePath, "step.second.output")
        assertThat(outputPath).isRegularFile()
        assertThat(outputPath).hasContent("STDOUT hello world")
    }

    @Nested
    inner class Calls {

        @Test
        fun failOnMissingConfigFile() {
            System.setProperty("PEBUILD_FILE", "unknown.pbuild.yml")

            try {
                Main.fromJava(arrayOf("run"), outP, errP)

                fail<Any>("should fail on missing config")
            } catch (e: CommandLine.PicocliException) {
                assertThat(e.message).contains("can not find config file unknown.pbuild.yml")
            }
        }

        // TODO hangs on Linux, when shellexecutor init fails to start

        @Test
        fun `should show help`() {
            Main.fromJava(arrayOf("-h"), outP, errP)

            val output = out.toString()
            assertThat(output).isNotEmpty()
            assertThat(output).contains("-h")
            assertThat(output).contains("--help")
        }
    }
}