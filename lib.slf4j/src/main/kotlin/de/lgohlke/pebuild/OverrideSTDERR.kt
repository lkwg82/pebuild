package de.lgohlke.pebuild

import java.io.PrintStream

interface OverrideSTDERR {
    fun err(err: PrintStream)
}
