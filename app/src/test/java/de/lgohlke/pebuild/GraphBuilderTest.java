package de.lgohlke.pebuild;

import de.lgohlke.pebuild.graph.ExecutionGraph2;
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GraphBuilderTest {

    private String yaml = "" +
            "options: \n" +
            "  timeout: 140s\n" +
            "\n" +
            "steps:\n" +
            "- name: demo\n" +
            "  command: 'date'\n" +
            "- name: sleep\n" +
            "  command: 'sleep 2'\n" +
            "  timeout: 10s\n" +
            "  waitfor: ['demo']";
    private ExecutionGraph2 graph = GraphBuilder.build(yaml);
    private Map<String, StepExecutor> jobMap = new HashMap<>();

    @BeforeEach
    void setUp() {
        graph.getSteps()
             .forEach(j -> jobMap.put(j.getName(), j));
    }

    @Test
    void shouldHaveCorrectOrder() {
        assertThat(graph.toString()).isEqualTo("(demo [])->(sleep [demo])");
    }

    @Test
    void shouldHaveConfiguredExecutor() {
        val demo = jobMap.get("demo");

        assertThat(demo.getCommand()).isEqualTo("date");

        val sleep = jobMap.get("sleep");

        assertThat(sleep.getCommand()).isEqualTo("sleep 2");
        assertThat(sleep.getTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void shouldHaveConfiguredTimeout() {
        assertThat(graph.getTimeout()).isEqualTo(Duration.ofSeconds(140));
    }

    @Test
    void failOnMissingReferencedWaitForJob() {
        String config = "" +
                "steps:\n" +
                " - name: test\n" +
                "   command: date\n" +
                "   waitfor: ['missing']\n";

        Executable executable = () -> GraphBuilder.build(config);

        assertThrows(ReferencedJobMissingValidator.ReferencedJobsMissing.class, executable);
    }
}
