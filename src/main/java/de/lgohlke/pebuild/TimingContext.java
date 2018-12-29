package de.lgohlke.pebuild;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class TimingContext {
    private final @NonNull String jobName;
    private final long startTimeMillis;
    private final long endTimeMillis;
}
