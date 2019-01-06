package de.lgohlke.pebuild.slf4j

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.*

internal class PELoggerTest {
    private val outStream = ByteArrayOutputStream()
    private val properties = Properties()
    private val logger = PELogger(clazz = "x", outStream = outStream, properties = properties)

    @Test
    internal fun `should not log when default INFO`() {
        logger.info("asdsad3")

        assertOutput().isEmpty()
    }

    @Test
    internal fun `should not log when default DEBUG`() {
        logger.debug("asdsad1")

        assertOutput().isEmpty()
    }

    @Test
    internal fun `should log when default WARN`() {
        logger.warn("asdsad2")

        assertOutput().endsWith("asdsad2\n")
    }

    @Test
    internal fun `should log when default ERROR`() {
        logger.error("asdsad4")

        assertOutput().endsWith("asdsad4\n")
    }

    @Nested
    inner class Loglevels {
        @Test
        fun `should log when set INFO globally`() {
            properties.setProperty(logger.logPrefix + ".defaultLogLevel", "INFO")

            val logger = PELogger(clazz = "x", outStream = outStream, properties = properties)

            logger.info("asdsad5")

            assertOutput().endsWith("asdsad5\n")
        }

        @Test
        fun `clazz config should override global config`() {
            properties.setProperty(logger.logPrefix + ".defaultLogLevel", "INFO")

            val logger = PELogger(clazz = "x", outStream = outStream, properties = properties)

            logger.info("asdsad5")

            assertOutput().endsWith("asdsad5\n")
        }
    }

    private fun assertOutput() = assertThat(String(outStream.toByteArray()))
}