package de.lgohlke.pebuild;

import de.lgohlke.pebuild.config.BuildConfigReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String... args) {
        graalvmTest();

        String cmd = String.join(" ", args);

        System.out.println("executing '" + cmd + "'");
        ShellExecutor.executeInheritedIO(cmd);
    }

    private static void graalvmTest() {
        // TODO yust for testing native-image compilation

        String yaml = "" +
                "options: \n" +
                "  timeout: 10m\n" +
                "\n" +
                "steps:\n" +
                "- name: demo\n" +
                "  command: 'date'\n" +
                "- name: sleep\n" +
                "  command: 'sleep 2'\n" +
                "  timeout: 10s\n" +
                "  waitfor: ['demo']";
        BuildConfigReader.parse(yaml);

        GraphBuilder.build(yaml);

        log.info("test");
    }
}