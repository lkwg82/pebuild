package de.lgohlke.ci;

import de.lgohlke.ci.config.BuildConfigReader;
import de.lgohlke.ci.config.dto.BuildConfig;
import de.lgohlke.ci.config.dto.Step;
import de.lgohlke.ci.graph.ExecutionGraph;
import de.lgohlke.ci.graph.Job;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphBuilderTest {

    private String yaml = "" +
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
    private ExecutionGraph graph = GraphBuilder.build(yaml);

    @Test
    void shouldHaveCorrectOrder() {
        assertThat(graph.toString()).isEqualTo("(demo [])->(sleep [demo])");
    }

    @Test
    void shouldHaveExecutor() {
        Map<String, Job> jobMap = new HashMap<>();
        graph.getJobs()
             .forEach(j -> jobMap.put(j.getName(), j));


        Job demo = jobMap.get("demo");
        StepExecutor executor = demo.getExecutor();

        assertThat(executor.getCommand()).isEqualTo("date");

        Job sleep = jobMap.get("sleep");
        StepExecutor executor2 = sleep.getExecutor();

        assertThat(executor2.getCommand()).isEqualTo("sleep 2");
        assertThat(executor2.getTimeout()).isEqualTo(Duration.ofSeconds(10));
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
                   .forEach(name -> {
                       Step step = stepMap.get(name);
                       StepExecutorConverter converter = new StepExecutorConverter(step);
                       ShellExecutor executor = converter.asShellExecutor();

                       // TODO
                       jobMap.put(name, new Job(name, executor, null));
                   });
        }
    }
}
