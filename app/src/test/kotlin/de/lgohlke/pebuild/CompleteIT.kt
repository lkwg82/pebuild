package de.lgohlke.pebuild

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class CompleteIT {

    @Test
    fun simpleRun() {
        Configuration.REPORT_DIRECTORY.overwrite("target/pebuild.d")
        System.setProperty("PEBUILD_FILE", "src/test/resources/integration/simple.pbuild.yml")

        Main().run()

        assertThat(Paths.get("target", "pebuild.d")).isDirectory()
        assertThat(Paths.get("target", "pebuild.d", "timings")).isRegularFile()
        assertThat(Paths.get("target", "pebuild.d", "step.second.output")).isRegularFile()
        assertThat(Paths.get("target", "pebuild.d", "step.second.output")).hasContent("STDOUT hello world")
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