package de.lgohlke.pebuild.cli

import de.lgohlke.pebuild.GraphBuilder
import de.lgohlke.pebuild.OverrideSTDERR
import de.lgohlke.pebuild.OverrideSTDOUT
import picocli.CommandLine
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.util.concurrent.Callable

@CommandLine.Command(name = "run")
class RunCommand : Callable<Void>,
                   OverrideSTDOUT,
                   OverrideSTDERR {

    private var out: PrintStream = System.out
    private var err: PrintStream = System.err

    @CommandLine.Option(names = ["--dry-run"],
                        description = ["only checks config"])
    var dryRun = false

    @CommandLine.Option(names = ["--file"],
                        description = ["path to config"])
    var configFile = "pebuild.yaml"

    override fun call(): Void? {

        val file = File(configFile)
        if (!file.isFile) {
            throw CommandLine.PicocliException("can not find config file $configFile")
        }

        val content = Files.readAllBytes(file.toPath())
        val yaml = String(content)

        GraphBuilder.build(yaml)

        if (dryRun) {
            out.println("- dry run -")
            out.write(yaml.toByteArray())
            out.println()
            return null // TODO needs exit code
        }

        return null
    }

    override fun out(out: PrintStream) {
        this.out = out
    }

    override fun err(err: PrintStream) {
        this.err = err
    }
}
