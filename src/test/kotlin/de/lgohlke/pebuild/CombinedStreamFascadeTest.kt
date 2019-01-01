package de.lgohlke.pebuild

import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Files
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CombinedStreamFascadeTest {
    private val tempDirectory = Files.newTemporaryFolder()
    private val path = Paths.get(tempDirectory.absolutePath, "step.output")

    private val oldSOUT = System.out
    private val newSOUT = ByteArrayOutputStream()

    @BeforeEach
    internal fun setUp() {
        System.setOut(PrintStream(newSOUT))
    }

    @AfterEach
    internal fun tearDown() {
        FileUtils.deleteDirectory(tempDirectory)
        System.setOut(PrintStream(oldSOUT))
    }

    @Test
    fun `should create step output file`() {
        val stdout = ByteArrayInputStream("".toByteArray())
        val stderr = ByteArrayInputStream("".toByteArray())

        runFascade("test", stdout, stderr)

        assertThat(path).exists()
    }

    @Test
    fun `should have STDOUT output collected in file`() {
        val stdout = ByteArrayInputStream("ok".toByteArray())
        val stderr = ByteArrayInputStream("".toByteArray())

        runFascade("test", stdout, stderr)

        val content = FileUtils.readFileToString(path.toFile(), Charset.defaultCharset())
        assertThat(content).contains("STDOUT ok")
    }

    @Test
    fun `should have STDOUT output printed to System out`() {
        val stdout = ByteArrayInputStream("ok".toByteArray())
        val stderr = ByteArrayInputStream("".toByteArray())

        runFascade("test", stdout, stderr)

        val content = newSOUT.toByteArray().toString(Charset.defaultCharset())
        assertThat(content).contains("[test] STDOUT ok")
    }

    @Test
    fun `should have STDERR output printed to System out`() {
        val stdout = ByteArrayInputStream("".toByteArray())
        val stderr = ByteArrayInputStream("err".toByteArray())

        runFascade("test", stdout, stderr)

        val content = newSOUT.toByteArray().toString(Charset.defaultCharset())
        assertThat(content).contains("[test] STDERR err")
    }

    @Test
    fun `should have STDERR output collected in file`() {
        val stdout = ByteArrayInputStream("".toByteArray())
        val stderr = ByteArrayInputStream("err".toByteArray())

        runFascade("test", stdout, stderr)

        val content = FileUtils.readFileToString(path.toFile(), Charset.defaultCharset())
        assertThat(content).contains("STDERR err")
    }

    private fun runFascade(jobName: String, stdout: ByteArrayInputStream, stderr: ByteArrayInputStream) {
        val fascade = CombinedStreamFascade(jobName, stdout, stderr, path)

        fascade.start()
        TimeUnit.MILLISECONDS.sleep(10 * 10)
        fascade.stop()
    }
}