package de.lgohlke.pebuild;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class JobTrigger {
    private final String jobName;

    private final Set<JobTriggerHandler> jobTriggerHandlers = new HashSet<>();

    public void registerHandler(JobTriggerHandler handler) {
        jobTriggerHandlers.add(handler);
    }

    public void triggerCompletion() {
        jobTriggerHandlers.forEach(handler -> handler.onComplete(jobName));
    }
}
