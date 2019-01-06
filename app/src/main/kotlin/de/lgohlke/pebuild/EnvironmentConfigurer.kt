package de.lgohlke.pebuild

import org.slf4j.LoggerFactory
import java.nio.file.Paths


internal class EnvironmentConfigurer(private val cwd: String = System.getProperty("user.dir")) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)

        fun mergeEnvironmentAndSystemProperties() {
            val getenv = System.getenv()

            getenv.forEach { name, value ->
                if (System.getProperties()
                                .contains(name)) {
                    log.debug("can not overwrite: $name")
                } else {
                    log.debug("add '$name' into System.properties")
                    System.setProperty(name, value)
                }
            }
        }
    }

    fun configureMeaningfullDefaults() {
        if (Paths.get(cwd, "pom.xml")
                        .toFile()
                        .exists()) {
            log.debug("recognized maven project")
            Configuration.REPORT_DIRECTORY.setIfMissing("target/pebuild.d")
        }
    }
}
