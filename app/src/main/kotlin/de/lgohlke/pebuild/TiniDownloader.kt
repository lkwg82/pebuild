package de.lgohlke.pebuild

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path

class TiniDownloader(private val path: String = System.getenv("PATH")) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    fun download(tini: Path): Boolean {
        if (tini.toFile().exists()) {
            log.debug("no need to download")
            return false;
        }
        log.debug("need to download")
        // ~23kb
        val url = URL("https://github.com/krallin/tini/releases/download/v0.18.0/tini-amd64")
        FileUtils.copyURLToFile(url, tini.toFile(), 2000, 2000)

        tini.toFile().setExecutable(true)
        return true;
    }

    fun tiniPath(): Pair<Boolean, String> {
        val builder = ProcessBuilder("which", "tini")
        val env = builder.environment()
        env["PATH"] = path
        val process = builder.start()

        val exitCode = process.waitFor()

        if (exitCode == 0) {
            val stringWriter = StringWriter()
            IOUtils.copy(process.inputStream, stringWriter, Charset.defaultCharset())
            return Pair(true, stringWriter.toString().trim())
        }
        return Pair(false, "")
    }

}