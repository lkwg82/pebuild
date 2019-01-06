package de.lgohlke.pebuild

import de.lgohlke.pebuild.cli.CLI
import de.lgohlke.pebuild.config.BuildConfigReader
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

        @JvmStatic
        fun main(args: Array<String>) {
            fromCLI(args)
        }

        private fun fromCLI(args: Array<String>) {
            run(args, RunAll().andExit(0), DefaultExceptionHandler<List<Any>>().andExit(1))
        }

        @JvmStatic
        fun fromJava(args: Array<String>) {
            run(args, RunAll(), DefaultExceptionHandler())
        }

        @JvmStatic
        private fun run(args: Array<String>,
                        handler1: CommandLine.AbstractParseResultHandler<List<Any>>,
                        exceptionHandler1: DefaultExceptionHandler<List<Any>>) {

            val cli = CLI()
            val cmd = CommandLine(cli)
            val handler = handler1.useOut(out).useAnsi(AUTO)
            val exceptionHandler = exceptionHandler1.useErr(err).useAnsi(AUTO)

            cmd.parseWithHandlers<List<Any>>(handler, exceptionHandler, *args)

            graalvmTest()
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