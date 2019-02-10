package de.lgohlke.pebuild.cli

import de.lgohlke.pebuild.Configuration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import picocli.CommandLine
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.security.SecureRandom

class RunCommandTest {
    private val outputStream = ByteArrayOutputStream()
    private val errStream = ByteArrayOutputStream()

    private val random = SecureRandom().nextInt()
    private val tempDir = Files.createTempDirectory("test_$random")
    private val outputDir = tempDir.resolve("output")

    @BeforeEach
    internal fun setUp() {
        Configuration.REPORT_DIRECTORY.overwrite(outputDir.toFile().absolutePath)
    }

    @Test
    fun `should check config`() {

        val yaml = """
options:
  timeout: 10m

steps:
- name: demo
  command: 'date'
- name: sleep
  command: 'sleep 2'
  timeout: 10s
  waitfor: ['demo']
        """

        val config = tempDir.resolve("pebuild.yaml")
        Files.write(config, yaml.toByteArray())

        cli("--dry-run", "--file", config.toAbsolutePath().toString())

        val out = String(outputStream.toByteArray())
        assertThat(out).contains(yaml)
    }

    @Test
    fun `should fail on missing file`() {

        try {
            cli()
            fail("should fail")
        } catch (e: CommandLine.PicocliException) {
            assertThat(e.message).contains("can not find config file")
        }
    }

    private fun cli(vararg args: String) {
        val out = PrintStream(outputStream)
        val err = PrintStream(errStream)

        val cmd = RunCommand()
        cmd.out(out)
        cmd.err(err)

        val cli = CommandLine(cmd, CommandFactory(out))
        val ansi = CommandLine.Help.Ansi.AUTO
        val handler = CommandLine.RunAll().useOut(out).useAnsi(ansi)
        val exceptionHandler = CommandLine.DefaultExceptionHandler<List<Any>>().useErr(err).useAnsi(ansi)

        cli.parseWithHandlers<List<Any>>(handler, exceptionHandler, *args)
    }
}