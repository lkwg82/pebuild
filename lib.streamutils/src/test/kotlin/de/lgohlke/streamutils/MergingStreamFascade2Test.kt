package de.lgohlke.streamutils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.util.concurrent.TimeUnit


class MergingStreamFascade2Test {
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

        fascade(inputStreams, System.out, arrayOf(fileOutputStream)).use {
            doSomeThing()
        }

        val content = String(fileOutputStream.toByteArray())
        assertThat(content).contains("STDOUT ok")
    }

    @RepeatedTest(10)
    fun `should have STDOUT output printed to System out`() {
        val inputStreams = createInputStreams("ok", "")

        fascade(inputStreams, stdout, arrayOf()).use {
            doSomeThing()
        }

        val content = String(stdoutIntern.toByteArray())
        assertThat(content).contains("[test] STDOUT ok")
    }

    private fun fascade(inputStreams: Array<PrefixedInputStream>,
                        stdout: PrintStream,
                        outputStreams: Array<OutputStream>): MergingStreamFascade2 {
        return MergingStreamFascade2
            .Builder()
            .name("test")
            .inputStreams(inputStreams)
            .stdout(stdout)
            .outputStreams(outputStreams)
            .build()
    }

    @RepeatedTest(10)
    fun `should have STDERR output printed to SystemOut`() {
        val inputStreams = createInputStreams("", "err")

        fascade(inputStreams, stdout, arrayOf()).use {
            doSomeThing()
        }

        val content = String(stdoutIntern.toByteArray())
        assertThat(content).contains("[test] STDERR err")
    }

    private fun doSomeThing() {
        log.warn("do something")
        TimeUnit.MILLISECONDS.sleep(50)
    }

    @RepeatedTest(10)
    fun `should have STDERR output collected in filestream`() {
        val inputStreams = createInputStreams("", "err")

        fascade(inputStreams, stdout, arrayOf(fileOutputStream)).use {
            doSomeThing()
        }

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