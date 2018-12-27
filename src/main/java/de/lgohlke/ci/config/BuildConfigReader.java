package de.lgohlke.ci.config;

import de.lgohlke.ci.config.dto.BuildConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class BuildConfigReader {
    public static BuildConfig parse(String yaml) {
        Constructor constructor = new Constructor(BuildConfig.class);
        return new Yaml(constructor).load(yaml);
    }
}
