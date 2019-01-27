package de.lgohlke.pebuild;

@Deprecated
public interface JobTriggerHandler {
    void onComplete(String jobName, TimingContext timingContext);
}
