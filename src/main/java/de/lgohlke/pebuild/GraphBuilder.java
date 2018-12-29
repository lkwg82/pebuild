package de.lgohlke.pebuild;

import de.lgohlke.pebuild.config.BuildConfigReader;
import de.lgohlke.pebuild.config.dto.BuildConfig;
import de.lgohlke.pebuild.config.dto.Step;
import de.lgohlke.pebuild.graph.ExecutionGraph;
import de.lgohlke.pebuild.graph.Job;
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public class GraphBuilder {
    static ExecutionGraph build(@NonNull String yaml) {

        BuildConfig buildConfig = BuildConfigReader.parse(yaml);

        Map<String, Step> stepMap = new HashMap<>();

        buildConfig.getSteps()
                   .forEach(step -> stepMap.put(step.getName(), step));

        Map<String, Job> jobMap = new HashMap<>();

        fillJobMapWithoutDependencies(stepMap, jobMap);
        resolveJobDependencies(stepMap, jobMap);

        ExecutionGraph.Builder builder = new ExecutionGraph.Builder().timeout(buildConfig.getOptions()
                                                                                         .getTimeoutAsDuration());
        jobMap.values()
              .forEach(builder::addJob);
        ExecutionGraph graph = builder.build();

        // register as handler
        graph.getJobs()
             .forEach(j -> {
                 StepExecutor executor = j.getExecutor();
                 JobTrigger jobTrigger = executor.getJobTrigger();
                 jobTrigger.registerHandler(graph);
             });

        return graph;
    }

    private static void resolveJobDependencies(Map<String, Step> stepMap, Map<String, Job> jobMap) {
        stepMap.forEach((name, step) -> step.getWaitfor()
                                            .forEach(waitJobName -> {
                                                Job waitJob = jobMap.get(waitJobName);
                                                if (null == waitJob) {
                                                    throw new ReferencedJobMissingValidator.ReferencedJobsMissing(
                                                            "referenced waitfor step '" + waitJobName + "' from step '" + name + "' is missing: forgotten/mistyped?");
                                                }
                                                Job job = jobMap.get(name);
                                                job.waitFor(waitJob);
                                            }));
    }

    private static void fillJobMapWithoutDependencies(Map<String, Step> stepMap, Map<String, Job> jobMap) {
        stepMap.keySet()
               .forEach(name -> {
                   Step step = stepMap.get(name);
                   StepExecutorConverter converter = new StepExecutorConverter(step, new JobTrigger(name));
                   ShellExecutor executor = converter.asShellExecutor();

                   jobMap.put(name, new Job(name, executor));
               });
    }
}