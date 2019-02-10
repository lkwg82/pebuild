package de.lgohlke.pebuild.cli

import picocli.CommandLine
import java.io.PrintStream

class CommandFactory(private val out: PrintStream) : CommandLine.IFactory {

    @Throws(Exception::class)
    override fun <T> create(cls: Class<T>): T {
        val newInstance = createNewInstance(cls)
        injectOutErr(newInstance)
        return newInstance

    }

    private fun <T> createNewInstance(cls: Class<T>): T {
        return try {
            cls.newInstance()
        } catch (ex: Exception) {
            val constructor = cls.getDeclaredConstructor()
            constructor.isAccessible = true
            constructor.newInstance()
        }
    }

    private fun <T> injectOutErr(instance: T) {
        if (instance is OverrideSTDOUT) {
            instance.out(out)
        }
    }
}