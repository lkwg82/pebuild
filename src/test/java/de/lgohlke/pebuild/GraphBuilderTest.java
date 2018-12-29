package de.lgohlke.pebuild;

import de.lgohlke.pebuild.graph.ExecutionGraph;
import de.lgohlke.pebuild.graph.Job;
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator;
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
    private ExecutionGraph graph = GraphBuilder.build(yaml);
    private Map<String, Job> jobMap = new HashMap<>();

    @BeforeEach
    void setUp() {
        graph.getJobs()
             .forEach(j -> jobMap.put(j.getName(), j));
    }

    @Test
    void shouldHaveCorrectOrder() {
        assertThat(graph.toString()).isEqualTo("(demo [])->(sleep [demo])");
    }

    @Test
    void shouldHaveConfiguredExecutor() {
        Job demo = jobMap.get("demo");
        StepExecutor executor = demo.getExecutor();

        assertThat(executor.getCommand()).isEqualTo("date");

        Job sleep = jobMap.get("sleep");
        StepExecutor executor2 = sleep.getExecutor();

        assertThat(executor2.getCommand()).isEqualTo("sleep 2");
        assertThat(executor2.getTimeout()).isEqualTo(Duration.ofSeconds(10));
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
