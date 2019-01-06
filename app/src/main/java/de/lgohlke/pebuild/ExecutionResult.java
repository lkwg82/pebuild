package de.lgohlke.pebuild;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public
class ExecutionResult {
    private final int exitCode;
    @Deprecated
    private final String stdout;
}
