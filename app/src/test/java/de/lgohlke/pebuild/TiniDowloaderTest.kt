package de.lgohlke.pebuild

import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.SecureRandom

class TiniDowloaderTest {
    private val tempDirectory = Files.createTempDirectory(SecureRandom().nextDouble().toString())
    private val tmpDir = tempDirectory.toFile().absolutePath
    private val tiniPath = Paths.get(tmpDir, "tini")

    @AfterEach
    internal fun tearDown() {
        FileUtils.deleteDirectory(tempDirectory.toFile())
    }

    @Test
    fun `should not find tini`() {
        val downloader = TiniDownloader("/bin")

        val (found, _) = downloader.tiniPath();

        assertThat(found).isFalse()
    }

    @Test
    fun `should find tini`() {
        Files.write(tiniPath, "".toByteArray())
        File(tiniPath.toFile().absolutePath).setExecutable(true)

        val downloader = TiniDownloader("/bin:$tmpDir")

        val (found, path) = downloader.tiniPath();

        assertThat(found).isTrue()
        assertThat(path).isEqualTo("$tmpDir/tini")
    }

    @Test
    fun `should download tini`() {
        TiniDownloader().download(tiniPath)

        assertThat(tiniPath).exists()
        assertThat(tiniPath).isExecutable()
    }
}