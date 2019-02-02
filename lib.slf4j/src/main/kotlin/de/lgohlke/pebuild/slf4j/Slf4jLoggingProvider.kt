package de.lgohlke.pebuild.slf4j

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class Slf4jLoggingProvider : SLF4JServiceProvider {

    private var loggerFactory: ILoggerFactory? = null
    private var markerFactory: IMarkerFactory? = null
    private var mdcAdapter: MDCAdapter? = null

    override fun getLoggerFactory(): ILoggerFactory? {
        return loggerFactory
    }

    override fun getMarkerFactory(): IMarkerFactory? {
        return markerFactory
    }

    override fun getMDCAdapter(): MDCAdapter? {
        return mdcAdapter
    }

    override fun getRequesteApiVersion(): String {
        return REQUESTED_API_VERSION
    }


    override fun initialize() {

        loggerFactory = PEBuildLoggerFactory()
        markerFactory = BasicMarkerFactory()
        mdcAdapter = NOPMDCAdapter()
    }

    companion object {
        /**
         * Declare the version of the SLF4J API this implementation is compiled against.
         * The value of this field is modified with each major release.
         */
        // to avoid constant folding by the compiler, this field must *not* be final
        var REQUESTED_API_VERSION = "1.8.99" // !final
    }
}