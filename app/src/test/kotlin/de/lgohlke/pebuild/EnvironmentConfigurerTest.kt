package de.lgohlke.pebuild

import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

internal class EnvironmentConfigurerTest {
    @BeforeEach
    fun setUp() {
        EnvironmentConfigurer.mergeEnvironmentAndSystemProperties()
    }

    @Test
    fun shouldMergeEnvAndSystemProperties() {
        assertThat(System.getProperty("HOME")).isNotEmpty()
    }

    @Test
    fun shouldPriotizeSystemProperties() {
        System.setProperty("HOME", "x")

        assertThat(System.getProperty("HOME")).isEqualTo("x")
    }

    @Nested
    internal inner class MeaningfulDefaults {

        private var environmentConfigurer: EnvironmentConfigurer? = null
        private var workingDirectory: String? = null

        @BeforeEach
        @Throws(IOException::class)
        fun setUp() {
            val directory = Files.createTempDirectory("aasdasd")
            workingDirectory = directory.toFile()
                    .canonicalPath
            environmentConfigurer = EnvironmentConfigurer(workingDirectory!!)
        }

        @AfterEach
        @Throws(IOException::class)
        fun tearDown() {
            FileUtils.deleteDirectory(File(workingDirectory!!))
        }

        @Test
        @Throws(IOException::class)
        fun configureForMaven() {
            Files.write(Paths.get(workingDirectory!!, "pom.xml"), "test".toByteArray())

            environmentConfigurer!!.configureMeaningfullDefaults()

            assertThat(Configuration.REPORT_DIRECTORY.value()).isEqualTo("target/pebuild.d")
        }
    }
}
