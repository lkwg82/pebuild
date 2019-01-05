package de.lgohlke.pebuild.cli

import picocli.CommandLine

@CommandLine.Command(name = "pebuild")
class CLI {
    fun toggleFlags() {
        val key = "org.slf4j.simpleLogger.log.de.lgohlke.pebuild"
        if (verbose) {
            System.setProperty(key, "INFO")
        }
        if (debug) {
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