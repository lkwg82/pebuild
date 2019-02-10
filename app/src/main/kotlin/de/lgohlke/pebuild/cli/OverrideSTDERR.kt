package de.lgohlke.pebuild.cli

import java.io.PrintStream

interface OverrideSTDERR {
    fun err(err: PrintStream)
}
