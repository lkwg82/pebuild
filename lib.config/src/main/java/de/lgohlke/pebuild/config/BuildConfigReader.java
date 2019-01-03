package de.lgohlke.pebuild.config;

import de.lgohlke.pebuild.config.dto.BuildConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class BuildConfigReader {
    public static BuildConfig parse(String yaml) {
        if (yaml.isEmpty()) {
            throw new EmptyConfig();
        }
        Constructor constructor = new Constructor(BuildConfig.class);
        BuildConfig buildConfig = new Yaml(constructor).load(yaml);

        validate(buildConfig);

        return buildConfig;
    }

    private static void validate(BuildConfig buildConfig) {
        if (null == buildConfig.getSteps()) {
            throw new MissingSteps();
        }
        if (buildConfig.getSteps()
                       .isEmpty()) {
            throw new MissingSteps();
        }

        buildConfig.getSteps()
                   .forEach(step -> {
                       if (null == step.getCommand()) {
                           throw new MissingCommandInStepException("command missing in step:" + step.getName());
                       }
                   });
    }

    static class MissingCommandInStepException extends RuntimeException {
        MissingCommandInStepException(String s) {
            super(s);
        }
    }

    static class EmptyConfig extends RuntimeException {
        EmptyConfig() {
            super("empty config provided");
        }
    }

    static class MissingSteps extends RuntimeException {
        MissingSteps() {
            super("missing steps");
        }
    }
}
