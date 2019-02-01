package de.lgohlke.streamutils;

import lombok.RequiredArgsConstructor;

import java.io.InputStream;

@RequiredArgsConstructor
public
class PrefixedInputStream {
    private final InputStream stream;
    private final String prefix;

    public InputStream getStream() {
        return stream;
    }

    public String getPrefix() {
        return prefix;
    }
}
