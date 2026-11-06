package io.github.ahmedsh10.shiftwise.service;

import java.util.List;

public class RuleViolationException extends RuntimeException {

    private final List<RuleViolation> violations;

    public RuleViolationException(List<RuleViolation> violations) {
        super(violations.get(0).message());
        this.violations = List.copyOf(violations);
    }

    public List<RuleViolation> getViolations() {
        return violations;
    }
}
