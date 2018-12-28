package de.lgohlke.ci;

import de.lgohlke.ci.config.BuildConfigReader;
import de.lgohlke.ci.config.dto.BuildConfig;
import de.lgohlke.ci.config.dto.Step;
import de.lgohlke.ci.graph.ExecutionGraph;
import de.lgohlke.ci.graph.Job;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphBuilderTest {
    @Test
    void shouldBuildFromConfigAGraph() {
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

        ExecutionGraph graph = GraphBuilder.build(yaml);

        assertThat(graph.toString()).isEqualTo("(demo []) -> (sleep [demo])");
    }

    public static class GraphBuilder {
        static ExecutionGraph build(String yaml) {

            BuildConfig buildConfig = BuildConfigReader.parse(yaml);

            Map<String, Step> stepMap = new HashMap<>();

            buildConfig.getSteps()
                       .forEach(step -> stepMap.put(step.getName(), step));

            Map<String, Job> jobMap = new HashMap<>();

            fillJobMapWithoutDependencies(stepMap, jobMap);
            resolveJobDependencies(stepMap, jobMap);

            ExecutionGraph.Builder builder = new ExecutionGraph.Builder();
            jobMap.values()
                  .forEach(builder::addJob);
            return builder.build();
        }

        private static void resolveJobDependencies(Map<String, Step> stepMap, Map<String, Job> jobMap) {
            stepMap.forEach((name, step) -> {
                step.getWaitfor()
                    .forEach(waitJobName -> {
                        Job waitJob = jobMap.get(waitJobName);
                        Job job = jobMap.get(name);
                        job.waitFor(waitJob);
                    });
            });
        }

        private static void fillJobMapWithoutDependencies(Map<String, Step> stepMap, Map<String, Job> jobMap) {
            stepMap.keySet()
                   .forEach(name -> jobMap.put(name, new Job(name)));
        }
    }
}
