package de.lgohlke.pebuild.slf4j

import org.slf4j.ILoggerFactory
import org.slf4j.Logger

class PEBuildLoggerFactory : ILoggerFactory {
    override fun getLogger(name: String): Logger {
        return PELogger(name)
    }
}