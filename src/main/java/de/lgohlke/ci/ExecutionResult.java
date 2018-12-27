package de.lgohlke.ci;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
class ExecutionResult {
    private final int exitCode;
    private final String stdout;
}
