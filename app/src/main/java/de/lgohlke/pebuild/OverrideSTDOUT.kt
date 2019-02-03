package de.lgohlke.pebuild

import java.io.PrintStream

interface OverrideSTDOUT {
    fun out(out: PrintStream)
}
