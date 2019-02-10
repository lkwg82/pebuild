package de.lgohlke.pebuild

import de.lgohlke.pebuild.cli.CLI
import picocli.CommandLine
import picocli.CommandLine.DefaultExceptionHandler
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.IFactory
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
            run(args, RunAll().andExit(0), DefaultExceptionHandler<List<Any>>().andExit(1))
        }

        fun fromJava(args: Array<String>,
                     out: PrintStream,
                     err: PrintStream) {
            run(args, RunAll(), DefaultExceptionHandler(), out, err)
        }

        @JvmStatic
        private fun run(args: Array<String>,
                        handler1: CommandLine.AbstractParseResultHandler<List<Any>>,
                        exceptionHandler1: DefaultExceptionHandler<List<Any>>,
                        out: PrintStream = System.out,
                        err: PrintStream = System.err) {

            val cli = CLI()
            val cmd = CommandLine(cli, CommandFactory(out))
            val handler = handler1.useOut(out).useAnsi(AUTO)
            val exceptionHandler = exceptionHandler1.useErr(err).useAnsi(AUTO)

            cmd.parseWithHandlers<List<Any>>(handler, exceptionHandler, *args)
        }

        class CommandFactory(private val out: PrintStream) : IFactory {

            @Throws(Exception::class)
            override fun <T> create(cls: Class<T>): T {
                return try {
                    val newInstance = cls.newInstance()
                    injectOutErr(newInstance)
                    newInstance
                } catch (ex: Exception) {
                    val constructor = cls.getDeclaredConstructor()
                    constructor.isAccessible = true
                    val newInstance = constructor.newInstance()
                    injectOutErr(newInstance)
                    newInstance
                }

            }

            private fun <T> injectOutErr(instance: T) {
                if (instance is OverrideSTDOUT) {
                    (instance as OverrideSTDOUT).out(out)
                }
            }
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