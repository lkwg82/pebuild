package de.lgohlke.streamutils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream


class MergingStreamFascadeTest {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)

        init {
            System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.MergingStreamFascade", "DEBUG")
            System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.NotifyWaiter", "DEBUG")

            System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
            System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true")
            System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss:SSS")
        }
    }

    private val stdoutIntern = ByteArrayOutputStream()
    private val stdout = PrintStream(stdoutIntern, true)
    private val fileOutputStream = ByteArrayOutputStream()

    @RepeatedTest(10)
    fun `should have STDOUT output collected in file`() {
        val inputStreams = createInputStreams("ok", "")

        installFascade(inputStreams, System.out, arrayOf(fileOutputStream))

        val content = String(fileOutputStream.toByteArray())
        assertThat(content).contains("STDOUT ok")
    }

    @RepeatedTest(10)
    fun `should have STDOUT output printed to System out`() {
        val inputStreams = createInputStreams("ok", "")

        installFascade(inputStreams, stdout, arrayOf())

        val content = String(stdoutIntern.toByteArray())
        assertThat(content).contains("[test] STDOUT ok")
    }

    private fun installFascade(inputStreams: Array<PrefixedInputStream>,
                               stdout: PrintStream,
                               outputStreams: Array<OutputStream>): MergingStreamFascade {
        val fascade2 = MergingStreamFascade("test", inputStreams, stdout, outputStreams)
        fascade2.install()
        return fascade2
    }

    @RepeatedTest(10)
    fun `should have STDERR output printed to SystemOut`() {
        val inputStreams = createInputStreams("", "err")

        installFascade(inputStreams, stdout, arrayOf())

        val content = String(stdoutIntern.toByteArray())
        assertThat(content).contains("[test] STDERR err")
    }

    @RepeatedTest(10)
    fun `should have STDERR output collected in filestream`() {
        val inputStreams = createInputStreams("", "err")

        installFascade(inputStreams, stdout, arrayOf(fileOutputStream))

        val content = String(fileOutputStream.toByteArray())
        assertThat(content).contains("STDERR err")
    }

    private fun createInputStreams(stdout: String,
                                   stderr: String): Array<PrefixedInputStream> {
        val out = PrefixedInputStream(ByteArrayInputStream(stdout.toByteArray()), "STDOUT")
        val err = PrefixedInputStream(ByteArrayInputStream(stderr.toByteArray()), "STDERR")
        return arrayOf(out, err)
    }
}