package de.lgohlke.pebuild

import picocli.CommandLine
import java.time.Duration
import java.util.concurrent.Callable

@CommandLine.Command(name = "exec", aliases = ["e"])
class ExecCommand : Callable<Void> {
    override fun call(): Void? {
        val cmd = commands.joinToString(" ")
        ShellExecutor("test", cmd, Duration.ZERO, JobTrigger("test"))
                .runCommand()
        return null
    }

    @CommandLine.Parameters(index = "0..*")
    var commands: Array<String> = arrayOf()

}