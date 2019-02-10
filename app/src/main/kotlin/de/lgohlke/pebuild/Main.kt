package de.lgohlke.pebuild

import de.lgohlke.pebuild.cli.CLI
import picocli.CommandLine.DefaultExceptionHandler
import picocli.CommandLine.RunAll
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths


class Main {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            fromCLI(args)
        }

        private fun fromCLI(args: Array<String>) {
            CLI.run(args, RunAll().andExit(0), DefaultExceptionHandler<List<Any>>().andExit(1), CLI())
        }

        fun fromJava(args: Array<String>,
                     out: PrintStream,
                     err: PrintStream) {
            CLI.run(args, RunAll(), DefaultExceptionHandler(), CLI(), out, err)
        }

    }

    fun run() {
        Configuration.configureDefaults()
        EnvironmentConfigurer.mergeEnvironmentAndSystemProperties()
        Configuration.showConfig()

        val yamlConfig = readConfigFromFile()

        GraphBuilder.build(yamlConfig).execute()
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