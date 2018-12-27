package de.lgohlke.ci.config.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@NoArgsConstructor
public class Options {
    private Duration timeout = Duration.ofMinutes(10L);

    public void setTimeout(String duration) {
        timeout = new TimeoutOptionParser().parseString(duration);
    }

    protected Duration getTimeout() {
        return timeout;
    }
}
