package de.lgohlke.ci.config.dto;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeoutOptionParser {

    private final Pattern timeoutPattern = Pattern.compile("(\\d+)(s|m)");

    private final static Map<String, ChronoUnit> UNIT_MAP = new HashMap<>();

    static {
        UNIT_MAP.put("s", ChronoUnit.SECONDS);
        UNIT_MAP.put("m", ChronoUnit.MINUTES);
    }

    public Duration parseString(String duration) {

        Matcher matcher = timeoutPattern.matcher(duration);

        if (matcher.matches()) {
            int value = Integer.valueOf(matcher.group(1));
            String unit = matcher.group(2);
            return Duration.of(value, UNIT_MAP.get(unit));
        } else {
            throw new IllegalArgumentException("could not match: " + duration);
        }
    }
}
