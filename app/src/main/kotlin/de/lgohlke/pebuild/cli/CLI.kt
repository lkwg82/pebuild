package de.lgohlke.pebuild.cli

import de.lgohlke.pebuild.EnvironmentConfigurer
import de.lgohlke.pebuild.ExecCommand
import picocli.CommandLine
import java.util.*
import java.util.concurrent.Callable

@CommandLine.Command(name = "pebuild",
                     subcommands = [ExecCommand::class, CommandLine.HelpCommand::class],
                     mixinStandardHelpOptions = true)
class CLI(private val prop: Properties = System.getProperties()) : Callable<Void> {
    override fun call(): Void? {
        decideAboutLogging()

        EnvironmentConfigurer.mergeEnvironmentAndSystemProperties()
        EnvironmentConfigurer().configureMeaningfullDefaults()
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