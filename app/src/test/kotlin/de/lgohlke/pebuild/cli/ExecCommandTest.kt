package de.lgohlke.pebuild.cli

import de.lgohlke.pebuild.Configuration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import picocli.CommandLine
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.security.SecureRandom

class ExecCommandTest {
    private val outputStream = ByteArrayOutputStream()

    @BeforeEach
    internal fun setUp() {
        Configuration.REPORT_DIRECTORY.overwrite("target")
    }

    @Test
    fun exec() {
        val random = SecureRandom().nextInt()

        cli(arrayOf("exec", "-e", "echo huhu $random"))

        val out = String(outputStream.toByteArray())
        assertThat(out).isEqualTo("[test] STDOUT huhu $random\n")
    }

    private fun cli(args: Array<String>) {
        val exec = ExecCommand()
        exec.out(PrintStream(outputStream))
        CommandLine.call(exec, *args)
    }
}