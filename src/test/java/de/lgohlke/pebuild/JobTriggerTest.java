package de.lgohlke.pebuild;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobTriggerTest {
    private JobTrigger notifier = new JobTrigger("test");

    @Test
    void shouldTriggerCompletion() {
        List<String> triggeredJobs = new ArrayList<>();

        JobTriggerHandler triggerHandler = new JobTriggerHandler() {
            @Override
            public void onComplete(String jobName, StepExecutor.TimeContext timeContext) {
                triggeredJobs.add(jobName);
            }
        };

        notifier.registerHandler(triggerHandler);

        notifier.triggerCompletion(new StepExecutor.TimeContext(0, 0));

        assertThat(triggeredJobs).containsExactly("test");
    }
}