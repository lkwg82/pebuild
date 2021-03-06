package de.lgohlke.pebuild;

import de.lgohlke.pebuild.config.BuildConfigReader;
import de.lgohlke.pebuild.config.dto.BuildConfig;
import de.lgohlke.pebuild.config.dto.Step;
import de.lgohlke.pebuild.graph.ExecutionGraph;
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator;
import lombok.NonNull;
import lombok.val;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class GraphBuilder {
    public static ExecutionGraph build(@NonNull String yaml) {

        BuildConfig buildConfig = BuildConfigReader.parse(yaml);

        Map<String, Step> stepMap = new HashMap<>();

        buildConfig.getSteps()
                   .forEach(step -> stepMap.put(step.getName(), step));

        Map<String, StepExecutor> jobMap = new HashMap<>();

        fillJobMapWithoutDependencies(stepMap, jobMap);
        resolveJobDependencies(stepMap, jobMap);

        Duration timeout = buildConfig.getOptions().getTimeoutAsDuration();

        return new ExecutionGraph.Builder().timeout(timeout)
                                           .addJobs(jobMap.values().toArray(new StepExecutor[]{}))
                                           .build();
    }

    private static void resolveJobDependencies(Map<String, Step> stepMap, Map<String, StepExecutor> jobMap) {
        stepMap.forEach((name, step) -> step.getWaitfor()
                                            .forEach(waitJobName -> {
                                                val waitJob = jobMap.get(waitJobName);
                                                if (null == waitJob) {
                                                    throw new ReferencedJobMissingValidator.ReferencedJobsMissing(
                                                            "referenced waitfor step '" + waitJobName + "' from step '" + name + "' is missing: forgotten/mistyped?");
                                                }
                                                val job = jobMap.get(name);
                                                job.waitFor(waitJob);
                                            }));
    }

    private static void fillJobMapWithoutDependencies(Map<String, Step> stepMap, Map<String, StepExecutor> jobMap) {
        stepMap.keySet()
               .forEach(name -> {
                   val step = stepMap.get(name);
                   val converter = new StepExecutorConverter(step);
                   val executor = converter.asShellExecutor();

                   jobMap.put(name, executor);
               });
    }
}
