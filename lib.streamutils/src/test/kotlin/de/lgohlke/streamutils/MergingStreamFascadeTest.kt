package de.lgohlke.streamutils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.util.concurrent.TimeUnit


class MergingStreamFascadeTest {
    companion object {
        init {
            System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.MergingStreamFascade", "DEBUG")
        }
    }

    private val stdoutIntern = ByteArrayOutputStream()
    private val stdout = PrintStream(stdoutIntern, true)
    private val fileOutputStream = ByteArrayOutputStream()

    @RepeatedTest(100)
    fun `should have STDOUT output collected in file`() {
        val inputStreams = createInputStreams("ok", "")

        MergingStreamFascade.create("test", inputStreams, stdout, arrayOf<OutputStream>(fileOutputStream)).use {
            doSomeThing()
        }

        val content = String(fileOutputStream.toByteArray())
        assertThat(content).contains("STDOUT ok")
    }

    @RepeatedTest(100)
    fun `should have STDOUT output printed to System out`() {
        val inputStreams = createInputStreams("ok", "")

        MergingStreamFascade.create("test", inputStreams, stdout, arrayOf()).use {
            doSomeThing()
        }

        val content = String(stdoutIntern.toByteArray())
        assertThat(content).contains("[test] STDOUT ok")
    }

    @RepeatedTest(100)
    fun `should have STDERR output printed to SystemOut`() {
        val inputStreams = createInputStreams("", "err")

        MergingStreamFascade.create("test", inputStreams, stdout, arrayOf()).use {
            doSomeThing()
        }

        val content = String(stdoutIntern.toByteArray())
        assertThat(content).contains("[test] STDERR err")
    }

    private fun doSomeThing() {
        TimeUnit.MILLISECONDS.sleep(50);
    }

    @RepeatedTest(100)
    fun `should have STDERR output collected in filestream`() {
        val inputStreams = createInputStreams("", "err")

        MergingStreamFascade.create("test", inputStreams, stdout, arrayOf<OutputStream>(fileOutputStream)).use {
            doSomeThing()
        }

        val content = String(fileOutputStream.toByteArray())
        assertThat(content).contains("STDERR err")
    }

    private fun createInputStreams(stdout: String, stderr: String): Array<PrefixedInputStream> {
        val out = PrefixedInputStream(ByteArrayInputStream(stdout.toByteArray()), "STDOUT")
        val err = PrefixedInputStream(ByteArrayInputStream(stderr.toByteArray()), "STDERR")
        return arrayOf(out, err)
    }

}