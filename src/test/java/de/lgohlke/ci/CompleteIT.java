package de.lgohlke.ci;

import de.lgohlke.ci.graph.ExecutionGraph;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;

class CompleteIT {
    @Test
    void name() {
        String content = loadTestConfig("integration/simple.pbuild.yml");

        ExecutionGraph graph = GraphBuilder.build(content);

        graph.execute();
    }

    private String loadTestConfig(String file) {
        URL resource = getClass().getClassLoader()
                                 .getResource(file);
        if (resource == null) {
            throw new IllegalStateException();
        }
        return Files.contentOf(Paths.get(resource.getFile())
                                    .toFile(), Charset.defaultCharset());
    }
}
