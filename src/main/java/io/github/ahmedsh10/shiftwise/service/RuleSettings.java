package io.github.ahmedsh10.shiftwise.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "shiftwise.rules")
public record RuleSettings(Integer minRestHours) {

    public static final RuleSettings DEFAULTS = new RuleSettings(11);

    public RuleSettings {
        if (minRestHours == null) {
            minRestHours = 11;
        }
        if (minRestHours < 0 || minRestHours > 24) {
            throw new IllegalStateException("shiftwise.rules.min-rest-hours must be between 0 and 24");
        }
    }

    public Duration minRest() {
        return Duration.ofHours(minRestHours);
    }
}
