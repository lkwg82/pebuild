package de.lgohlke.pebuild;

public interface JobTriggerHandler {
    void onComplete(String jobName, StepExecutor.TimeContext timeContext);
}
