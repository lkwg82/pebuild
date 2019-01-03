package de.lgohlke.pebuild;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
class ExecutionResult {
    private final int exitCode;
    private final String stdout;
}
