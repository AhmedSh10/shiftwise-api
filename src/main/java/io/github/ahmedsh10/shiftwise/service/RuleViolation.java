package io.github.ahmedsh10.shiftwise.service;

/** A scheduling rule that a proposed assignment breaks. */
public record RuleViolation(Rule rule, String message) {

    public enum Rule {
        EMPLOYEE_INACTIVE,
        NOT_QUALIFIED_FOR_AREA,
        OVERLAPPING_SHIFT
    }
}
