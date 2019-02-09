package de.lgohlke.pebuild.cli

import de.lgohlke.pebuild.OverrideSTDOUT
import de.lgohlke.pebuild.ShellExecutor
import picocli.CommandLine
import java.io.PrintStream
import java.time.Duration
import java.util.concurrent.Callable

@CommandLine.Command(name = "exec", aliases = ["e"])
class ExecCommand : Callable<Void>, OverrideSTDOUT {
    var out: PrintStream = System.out

    @CommandLine.Option(names = ["-e"],
                        description = ["suppresses exiting with actual code (only for testing purposes"])
    var suppressExitCode = false

    override fun call(): Void? {
        val cmd = commands.joinToString(" ")
        val executor = ShellExecutor("test", cmd, Duration.ofDays(999), out)
        val executionResult = executor.runCommand()

        if (!suppressExitCode) {
            System.exit(executionResult.exitCode)
        }

        return null
    }

    @CommandLine.Parameters(index = "0..*")
    var commands: Array<String> = arrayOf()

    override fun out(out: PrintStream) {
        this.out = out
    }
}