package de.lgohlke.pebuild;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ExecutionResult {
    public final int exitCode;
    @Deprecated
    private final String stdout;
}
