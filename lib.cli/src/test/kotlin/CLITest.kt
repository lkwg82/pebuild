import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import picocli.CommandLine

internal class CLITest {
    @Test
    fun test() {
        val args = arrayOf("-h")

        val (cli, commandLine, parseResult) = createCLIParsing(args)
        if (parseResult.isUsageHelpRequested) {
            commandLine.usage(System.out)
        }

        assertThat(cli.helpRequested).isTrue()
    }

    private fun createCLIParsing(args: Array<String>): Triple<CLI, CommandLine, CommandLine.ParseResult> {
        // https://picocli.info/#_registering_subcommands_programmatically
        val cli = CLI()
        val commandLine = CommandLine(cli)
        val parseResult = commandLine.parseArgs(*args)

        cli.toggleFlags()
        return Triple(cli, commandLine, parseResult)
    }

    @Test
    fun `toggle verbose (short)`() {
        val key = "org.slf4j.simpleLogger.log.de.lgohlke.pebuild"
        System.getProperties().remove(key)
        val args = arrayOf("--verbose")

        createCLIParsing(args)

        assertThat(System.getProperties()).containsEntry(key, "INFO")
    }

    @Test
    fun `toggle verbose (long)`() {
        val key = "org.slf4j.simpleLogger.log.de.lgohlke.pebuild"
        System.getProperties().remove(key)
        val args = arrayOf("--verbose")

        createCLIParsing(args)

        assertThat(System.getProperties()).containsEntry(key, "INFO")
    }

    @Test
    fun `toggle debug (short)`() {
        val key = "org.slf4j.simpleLogger.log.de.lgohlke.pebuild"
        System.getProperties().remove(key)
        val args = arrayOf("--debug")

        createCLIParsing(args)

        assertThat(System.getProperties()).containsEntry(key, "DEBUG")
    }

    @Test
    fun `toggle debug (long)`() {
        val key = "org.slf4j.simpleLogger.log.de.lgohlke.pebuild"
        System.getProperties().remove(key)
        val args = arrayOf("--debug")

        createCLIParsing(args)

        assertThat(System.getProperties()).containsEntry(key, "DEBUG")
    }

    @CommandLine.Command(name = "pebuild")
    internal inner class CLI {
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
}