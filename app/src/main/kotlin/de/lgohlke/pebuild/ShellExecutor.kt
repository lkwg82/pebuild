package de.lgohlke.pebuild

import de.lgohlke.streamutils.MergingStreamFascade
import de.lgohlke.streamutils.PrefixedInputStream
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit

class ShellExecutor(name: String,
                    command: String,
                    timeout: Duration = Duration.ofDays(999),
                    val systemOut: PrintStream = System.out) : StepExecutor(name, command, timeout) {

    constructor(name: String,
                command: String) : this(name, command, Duration.ofDays(999), System.out)

    constructor(name: String,
                command: String,
                timeout: Duration) : this(name, command, timeout, System.out)

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    private var process: Process? = null

    @Throws(Exception::class)
    override fun runCommand(): ExecutionResult {
        process = startProcess()
        val processs = process ?: throw IllegalStateException("process is null")

        val outputFile = prepareOutputFile()

        try {
            FileOutputStream(outputFile.toFile()).use { fout ->
                val stdout = PrefixedInputStream(processs.inputStream, "STDOUT")
                val stderr = PrefixedInputStream(processs.errorStream, "STDERR")

                val inputStreams = arrayOf(stdout, stderr)
                val outputStreams = arrayOf<OutputStream>(fout)

                MergingStreamFascade(name, inputStreams, systemOut, outputStreams).install()

                val exitCode = waitForProcess(processs)
                log.debug("finished with exit code {}", exitCode)

                return ExecutionResult(exitCode)
            }
        } finally {
            process = null
        }
    }

    override fun cancel() {
        if (null == process) {
            log.debug("tried to cancel a not running instance")
            return
        }
        process!!.destroy()
    }

    @Throws(InterruptedException::class)
    private fun waitForProcess(process: Process): Int {
        val timeout = timeout
        if (timeout.isZero) {
            return process.waitFor()
        } else {
            if (process.waitFor(timeout.seconds, TimeUnit.SECONDS)) {
                log.debug("exits before timeout")
                return process.exitValue()
            } else {
                log.debug("timout")
                process.destroy()
                return if (process.waitFor(1, TimeUnit.SECONDS)) {
                    log.debug("terminated after timeout")
                    process.exitValue()
                } else {
                    log.warn("failed to terminate")
                    process.destroyForcibly()
                    log.warn("killed")

                    process.exitValue()
                }
            }
        }
    }

    private fun prepareOutputFile(): Path {
        val filename = "step.$name.output"

        if (Configuration.REPORT_DIRECTORY.value().isEmpty()) {
            Configuration.REPORT_DIRECTORY.setIfMissing(System.getProperty("user.dir"))
        }

        val outputFile = Paths.get(Configuration.REPORT_DIRECTORY.value(), filename)
        val directory = outputFile.parent.toFile()
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return outputFile
    }

    @Throws(IOException::class)
    private fun startProcess(): Process {
        log.info("executing: '{}'", command)
        val processBuilder = prepareExecutionContext()

        log.debug("starting")
        val process = processBuilder.start()

        if (process.isAlive) {
            log.debug("raw input: {}", command)
            val rawInput = command + "\n"
            val cmd = rawInput.toByteArray()

            process.outputStream.use { outputStream -> outputStream.write(cmd) }
        } else {
            log.warn("process already exited with code: {}", process.exitValue())
        }
        return process

    }

    private fun prepareExecutionContext(): ProcessBuilder {
        val osDetector = OSDetector()
        if (osDetector.isLinux) {
            // TODO handling with tini is very instable
            //            val pathToTini = prepareTini();
            //            String[] wrappedInShell = new String[]{pathToTini, "-s", "-vvvv", "-w", "-g", "sh"};
            val wrappedInShell = arrayOf("sh")
            log.debug("raw command is '{}'", wrappedInShell.joinToString(" "))
            return ProcessBuilder(*wrappedInShell)
        }

        if (osDetector.isMac) {
            val wrappedInShell = arrayOf("sh")
            log.debug("raw command is '{}'", wrappedInShell.joinToString(" "))
            log.warn("" +
                             "dont have a reaper context, so the build " +
                             "can leave zombie processes behind " +
                             "(not yet implemented, see tini for linux)")
            return ProcessBuilder(*wrappedInShell)
        }

        log.error("your operating system is not supported (yet), consider a PR")
        System.exit(1)
        throw IllegalStateException("could not happen")
    }

    private fun prepareTini(): String {
        val path = System.getenv("PATH")
        val tinitDownloader = TiniDownloader(path)
        val (first, second) = tinitDownloader.tiniPath()
        if (first) {
            log.debug("has tini in PATH:{}", second)
            return second
        } else {
            log.debug("download tini")
            // TODO too specific to maven
            val tiniPath = Paths.get("target", "bin")
            File(tiniPath.toFile().absolutePath).mkdirs()
            val pathToTini = tiniPath.resolve("tini")
            tinitDownloader.download(pathToTini)

            return pathToTini.toFile().absolutePath
        }
    }

    private class OSDetector internal constructor() {

        internal val isMac: Boolean
            get() = OS.contains("mac")

        internal val isLinux: Boolean
            get() = OS.contains("linux")

        companion object {
            private val OS = System.getProperty("os.name").toLowerCase()
        }
    }
}