package de.lgohlke.pebuild.cli

import de.lgohlke.pebuild.Configuration
import de.lgohlke.pebuild.GraphBuilder
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
    var configFile = Configuration.FILE.value()

    override fun call(): Void? {

        val file = File(configFile)
        if (!file.isFile) {
            throw CommandLine.PicocliException("can not find config file $configFile")
        }

        val content = Files.readAllBytes(file.toPath())
        val yaml = String(content)

        val graph = GraphBuilder.build(yaml)

        if (dryRun) {
            out.println("- dry run -")
            out.write(yaml.toByteArray())
            out.println()
            return null // TODO needs exit code
        }

        graph.execute()

        return null
    }

    override fun out(out: PrintStream) {
        this.out = out
    }

    override fun err(err: PrintStream) {
        this.err = err
    }
}
