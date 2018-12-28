package de.lgohlke.ci;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class FinishNotifier {
    private final String jobName;

    private final Set<JobTriggerHandler> jobTriggerHandlers = new HashSet<>();


    public void registerHandler(JobTriggerHandler handler) {
        jobTriggerHandlers.add(handler);
    }

    public void triggerCompletion() {
        jobTriggerHandlers.forEach(handler -> handler.onComplete(jobName));
    }
}
