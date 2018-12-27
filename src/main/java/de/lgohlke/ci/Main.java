package de.lgohlke.ci;

import org.yaml.snakeyaml.Yaml;

public class Main {
    public static void main(String... args) {

        // TODO yust for testing native-image compilation
        new Yaml();

        String cmd = String.join(" ", args);

        System.out.println("executing '" + cmd + "'");
        ShellExecutor.executeInheritedIO(cmd);
    }
}
