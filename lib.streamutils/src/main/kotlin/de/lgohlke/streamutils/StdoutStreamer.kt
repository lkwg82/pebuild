package de.lgohlke.streamutils

import java.io.PrintStream

internal class StdoutStreamer(private val jobName: String,
                              private val stdout: PrintStream) : OutputStreamer(stdout) {
    override fun onNext(line: String?) {
        stdout.println("[$jobName] $line")
    }
}