package de.lgohlke.pebuild.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import picocli.CommandLine
import java.util.*

internal class CLITest {

    private val propertieS = Properties()

    private fun createCLIParsing(args: Array<String>) {
        val cli = CLI(propertieS)
        CommandLine.call(cli, *args)
    }

    private val key = "org.slf4j.simpleLogger.defaultLogLevel"

    @Test
    fun `toggle verbose (short)`() {
        val args = arrayOf("--verbose")

        createCLIParsing(args)

        assertThat(propertieS).containsEntry(key, "INFO")
    }

    @Test
    fun `toggle verbose (long)`() {
        val args = arrayOf("--verbose")

        createCLIParsing(args)

        assertThat(propertieS).containsEntry(key, "INFO")
    }

    @Test
    fun `toggle debug (short)`() {
        val args = arrayOf("--debug")

        createCLIParsing(args)

        assertThat(propertieS).containsEntry(key, "DEBUG")
    }

    @Test
    fun `toggle debug (long)`() {
        val args = arrayOf("--debug")

        createCLIParsing(args)

        assertThat(propertieS).containsEntry(key, "DEBUG")
    }
}