package de.lgohlke.pebuild;

import java.util.HashSet;
import java.util.Set;

public class JobTrigger {
    private final String jobName;

    private final Set<JobTriggerHandler> jobTriggerHandlers = new HashSet<>();

    public JobTrigger(String jobName) {
        this.jobName = jobName;
    }

    public void registerHandler(JobTriggerHandler handler) {
        jobTriggerHandlers.add(handler);
    }

    public void triggerCompletion(TimingContext timingContext) {
        jobTriggerHandlers.forEach(handler -> handler.onComplete(jobName, timingContext));
    }
}
