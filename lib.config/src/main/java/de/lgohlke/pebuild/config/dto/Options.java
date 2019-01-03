package de.lgohlke.pebuild.config.dto;

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

    public String getTimeout() {
        return timeout + "";
    }

    public Duration getTimeoutAsDuration() {
        return timeout;
    }
}
