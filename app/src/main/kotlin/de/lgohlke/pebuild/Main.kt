package de.lgohlke.pebuild

import de.lgohlke.pebuild.cli.CLI
import picocli.CommandLine.DefaultExceptionHandler
import picocli.CommandLine.RunAll
import java.io.PrintStream


class Main {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            CLI.run(args, RunAll().andExit(0), DefaultExceptionHandler<List<Any>>().andExit(1), CLI())
        }

        // this is only for tests to
        // - inject STDOUT and STDERR
        // - not to call System.exit
        fun fromJava(args: Array<String>,
                     out: PrintStream,
                     err: PrintStream) {
            CLI.run(args, RunAll(), DefaultExceptionHandler(), CLI(), out, err)
        }
    }
}