package de.lgohlke.ci.config.dto;

import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
public class Step {
    private String name = "step";
    private String command = "MISSING";
    private Duration timeout = Duration.ofMinutes(0);

    private List<String> waitfor = new ArrayList<>();

    public void setTimeout(String duration) {
        timeout = new TimeoutOptionParser().parseString(duration);
    }

    protected Duration getTimeout() {
        return timeout;
    }
}
