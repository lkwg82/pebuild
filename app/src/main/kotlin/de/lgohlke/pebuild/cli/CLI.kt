package de.lgohlke.pebuild.cli

import de.lgohlke.pebuild.Configuration
import de.lgohlke.pebuild.EnvironmentConfigurer
import picocli.CommandLine
import java.io.PrintStream
import java.util.*
import java.util.concurrent.Callable

@CommandLine.Command(name = "pebuild",
                     subcommands = [ExecCommand::class, RunCommand::class, CommandLine.HelpCommand::class],
                     mixinStandardHelpOptions = true)
class CLI(private val prop: Properties = System.getProperties()) : Callable<Void> {

    companion object {

        @JvmStatic
        fun run(args: Array<String>,
                handler1: CommandLine.AbstractParseResultHandler<List<Any>>,
                exceptionHandler1: CommandLine.DefaultExceptionHandler<List<Any>>,
                command: Callable<Void>,
                out: PrintStream = System.out,
                err: PrintStream = System.err) {

            val cmd = CommandLine(command, CommandFactory(out))
            val handler = handler1.useOut(out).useAnsi(CommandLine.Help.Ansi.AUTO)
            val exceptionHandler = exceptionHandler1.useErr(err).useAnsi(CommandLine.Help.Ansi.AUTO)

            cmd.parseWithHandlers<List<Any>>(handler, exceptionHandler, *args)
        }
    }

    override fun call(): Void? {
        decideAboutLogging()

        Configuration.configureDefaults()
        EnvironmentConfigurer.mergeEnvironmentAndSystemProperties()
        EnvironmentConfigurer().configureMeaningfullDefaults()
        Configuration.showConfig()


        return null
    }

    private fun decideAboutLogging() {
        val key = "org.slf4j.simpleLogger.defaultLogLevel"
        if (verbose) {
            prop.setProperty(key, "INFO")
        }
        if (debug) {
            prop.setProperty(key, "DEBUG")
        }
    }

    @CommandLine.Option(names = ["-v", "--verbose"], description = ["enable verbose logging"])
    var verbose = false

    @CommandLine.Option(names = ["-d", "--debug"], description = ["enable debug logging"])
    var debug = false
}