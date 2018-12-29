package de.lgohlke.pebuild;

import de.lgohlke.pebuild.config.BuildConfigReader;
import de.lgohlke.pebuild.graph.ExecutionGraph;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Main {
    public static void main(String... args) {
        EnvironmentConfigurer.mergeEnvironmentAndSystemProperties();

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

    @SneakyThrows
    public void run() {
        Configuration.configureDefaults();
        EnvironmentConfigurer.mergeEnvironmentAndSystemProperties();
        Configuration.showConfig();

        String content = readConfigFromFile();

        ExecutionGraph graph = GraphBuilder.build(content);

        graph.execute();
    }

    private String readConfigFromFile() throws IOException {
        String path = Configuration.FILE.value();
        if (!new File(path).exists()) {
            throw new IllegalStateException("missing config file: " + path);
        }
        Path path1 = Paths.get(path);
        return new String(Files.readAllBytes(path1));
    }
}
