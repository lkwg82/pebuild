package de.lgohlke.pebuild

import de.lgohlke.pebuild.cli.CLI
import de.lgohlke.pebuild.config.BuildConfigReader
import org.slf4j.LoggerFactory.getLogger
import picocli.CommandLine
import picocli.CommandLine.DefaultExceptionHandler
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.RunAll
import java.io.File
import java.io.IOException
import java.lang.System.err
import java.lang.System.out
import java.nio.file.Files
import java.nio.file.Paths


class Main {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = getLogger(javaClass.enclosingClass)


        @JvmStatic
        fun main(args: Array<String>) {
            graalvmTest()

            EnvironmentConfigurer.mergeEnvironmentAndSystemProperties()

            val cli = CLI()
            val cmd = CommandLine(cli)

            val handler = RunAll().useOut(out).useAnsi(AUTO)
            val exceptionHandler = DefaultExceptionHandler<List<Any>>().useErr(err).useAnsi(AUTO)

            val result = cmd.parseWithHandlers<List<Any>>(handler, exceptionHandler, *args)

            println(result)

            val threadSet = Thread.getAllStackTraces().keys
            for (t in threadSet) {
                println("thread: " + t.name)
            }
        }

        private fun graalvmTest() {
            // TODO yust for testing native-image compilation

            val yaml = "" +
                    "options: \n" +
                    "  timeout: 10m\n" +
                    "\n" +
                    "steps:\n" +
                    "- name: demo\n" +
                    "  command: 'date'\n" +
                    "- name: sleep\n" +
                    "  command: 'sleep 2'\n" +
                    "  timeout: 10s\n" +
                    "  waitfor: ['demo']"
            BuildConfigReader.parse(yaml)

            GraphBuilder.build(yaml)

            log.info("test")
        }
    }

    fun run() {
        Configuration.configureDefaults()
        EnvironmentConfigurer.mergeEnvironmentAndSystemProperties()
        Configuration.showConfig()

        val content = readConfigFromFile()

        val graph = GraphBuilder.build(content)

        graph.execute()
    }

    @Throws(IOException::class)
    private fun readConfigFromFile(): String {
        val path = Configuration.FILE.value()
        if (!File(path).exists()) {
            throw IllegalStateException("missing config file: $path")
        }
        val path1 = Paths.get(path)
        return String(Files.readAllBytes(path1))
    }


}