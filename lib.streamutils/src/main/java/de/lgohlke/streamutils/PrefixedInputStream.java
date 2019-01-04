package de.lgohlke.streamutils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;

@RequiredArgsConstructor
@Getter
public
class PrefixedInputStream {
    private final InputStream stream;
    private final String prefix;
}
