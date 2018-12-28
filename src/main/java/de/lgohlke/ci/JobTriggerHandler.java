package de.lgohlke.ci;

public interface JobTriggerHandler {
    void onComplete(String jobName);
}
