package de.lgohlke.pebuild

import org.apache.commons.io.FileUtils
import java.net.URL
import java.nio.file.Path

class TiniDownloader(private val path: String = System.getenv("PATH")) {
    fun hasTini(): Boolean {
        val builder = ProcessBuilder("which", "tini")
        val env = builder.environment()
        env["PATH"] = path
        val process = builder.start()

        return process.waitFor() == 0
    }

    fun download(tini: Path) {
        // ~23kb
        val url = URL("https://github.com/krallin/tini/releases/download/v0.18.0/tini-amd64")
        FileUtils.copyURLToFile(url, tini.toFile(), 2000, 2000)

        tini.toFile().setExecutable(true)
    }

}