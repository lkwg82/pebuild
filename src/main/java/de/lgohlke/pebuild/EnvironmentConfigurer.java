package de.lgohlke.pebuild;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
class EnvironmentConfigurer {
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
}
