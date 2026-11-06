package io.github.ahmedsh10.shiftwise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Duration;
import java.time.LocalTime;

/** One continuous block of work inside a shift. A split shift has two. */
@Embeddable
public class ShiftSegment {

    @Column(name = "start_time")
    private LocalTime start;

    @Column(name = "end_time")
    private LocalTime end;

    protected ShiftSegment() {
    }

    public ShiftSegment(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Segment end must be after start");
        }
        this.start = start;
        this.end = end;
    }

    public boolean overlaps(ShiftSegment other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }
}
