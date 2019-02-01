package de.lgohlke.pebuild

import picocli.CommandLine
import java.util.concurrent.Callable

@CommandLine.Command(name = "exec", aliases = ["e"], mixinStandardHelpOptions = true)
class ExecCommand : Callable<Void> {

    @CommandLine.Option(names = ["-e"],
                        description = ["suppresses exiting with actual code (only for testing purposes"])
    var suppressExitCode = false

    override fun call(): Void? {
        val cmd = commands.joinToString(" ")
        val executor = ShellExecutor("test", cmd)
        val executionResult = executor.runCommand()

        if (!suppressExitCode) {
            System.exit(executionResult.exitCode)
        }

        return null
    }

    @CommandLine.Parameters(index = "0..*")
    var commands: Array<String> = arrayOf()

}