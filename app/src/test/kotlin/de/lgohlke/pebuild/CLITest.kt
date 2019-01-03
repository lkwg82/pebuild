package de.lgohlke.pebuild

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import picocli.CommandLine

internal class CLITest {
    @Test
    fun test() {
        val args = arrayOf("-h")
        val cli = CLI()
// https://picocli.info/#_registering_subcommands_programmatically
        val commandLine = CommandLine(cli)
        val parseResult = commandLine.parseArgs(*args)
        if (parseResult.isUsageHelpRequested) {
            commandLine.usage(System.out)
        }

        assertThat(cli.helpRequested).isTrue()
    }

    @CommandLine.Command(name = "pebuild")
    internal inner class CLI {

        @CommandLine.Option(names = ["-h", "--help"], usageHelp = true, description = ["display a help message"])
        var helpRequested = false
    }
}