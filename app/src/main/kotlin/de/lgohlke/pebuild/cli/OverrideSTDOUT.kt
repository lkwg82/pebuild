package de.lgohlke.pebuild.cli

import java.io.PrintStream

interface OverrideSTDOUT {
    fun out(out: PrintStream)
}
