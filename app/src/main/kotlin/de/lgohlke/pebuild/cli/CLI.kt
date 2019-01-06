package de.lgohlke.pebuild.cli

import de.lgohlke.pebuild.ExecCommand
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.util.concurrent.Callable

@CommandLine.Command(name = "pebuild", subcommands = [ExecCommand::class])
class CLI : Callable<Void> {
    companion object Log {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun call(): Void? {
        toggleFlags()
//            val commandLine = CommandLine(this)
//            try {
//                val parseResult = commandLine.parseArgs(*args)
//
//                cli.toggleFlags()
//
//                if (commandLine.isUsageHelpRequested) {
//                    commandLine.usage(System.out)
//                    System.exit(0)
//                }
//
//                // TODO check exit codes
//            } catch (e: CommandLine.UnmatchedArgumentException) {
//                if (e.isUnknownOption) {
//                    System.err.println(e.message)
//                    commandLine.usage(System.err)
//                    System.exit(1)
//                }
//            }
        return null
    }

    fun toggleFlags() {
        val key = "org.slf4j.simpleLogger.log.de.lgohlke.pebuild"
        if (verbose) {
            System.setProperty(key, "INFO")
        }
        if (debug) {
            log.info("enable debugging")
            System.setProperty(key, "DEBUG")
        }
    }

    @CommandLine.Option(names = ["-h", "--help"], usageHelp = true, description = ["shows help"])
    var helpRequested = false

    @CommandLine.Option(names = ["-v", "--verbose"], description = ["enable verbose logging"])
    var verbose = false

    @CommandLine.Option(names = ["-d", "--debug"], description = ["enable debug logging"])
    var debug = false
}