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
            public void onComplete(String jobName, TimingContext timeContext) {
                triggeredJobs.add(jobName);
            }
        };

        notifier.registerHandler(triggerHandler);

        notifier.triggerCompletion(new TimingContext("name", 0, 0));

        assertThat(triggeredJobs).containsExactly("test");
    }
}