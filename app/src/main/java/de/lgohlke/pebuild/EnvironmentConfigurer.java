package de.lgohlke.pebuild;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.Map;

@Slf4j
class EnvironmentConfigurer {
    private final String cwd;

    EnvironmentConfigurer(String cwd) {
        this.cwd = cwd;
    }

    EnvironmentConfigurer() {
        this(System.getProperty("user.dir"));
    }

    static void mergeEnvironmentAndSystemProperties() {
        Map<String, String> getenv = System.getenv();

        getenv.forEach((name, value) -> {
            if (System.getProperties()
                      .contains(name)) {
                log.debug("can not overwrite: " + name);
            } else {
                log.debug("add '" + name + "' into System.properties");
                System.setProperty(name, value);
            }
        });
    }

    void configureMeaningfullDefaults() {
        if (Paths.get(cwd, "pom.xml")
                 .toFile()
                 .exists()) {
            log.debug("recognized maven project");
            Configuration.REPORT_DIRECTORY.setIfMissing("target/pebuild.d");
        }
    }
}
