package io.github.ahmedsh10.shiftwise.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A reusable shift pattern such as "Morning 08:00-12:00" or a split shift. */
@Entity
@Table(name = "shift_templates")
public class ShiftTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shift_segments", joinColumns = @JoinColumn(name = "shift_template_id"))
    @OrderColumn(name = "seq")
    private List<ShiftSegment> segments = new ArrayList<>();

    protected ShiftTemplate() {
    }

    public ShiftTemplate(String code, String name, List<ShiftSegment> segments) {
        this.code = code;
        this.name = name;
        this.segments = new ArrayList<>(segments);
    }

    /** True if any segment of this shift overlaps any segment of the other, on the same day. */
    public boolean overlaps(ShiftTemplate other) {
        for (ShiftSegment mine : segments) {
            for (ShiftSegment theirs : other.segments) {
                if (mine.overlaps(theirs)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Duration workedTime() {
        return segments.stream().map(ShiftSegment::duration).reduce(Duration.ZERO, Duration::plus);
    }

    public LocalTime firstStart() {
        return segments.stream().map(ShiftSegment::getStart).min(Comparator.naturalOrder()).orElseThrow();
    }

    public LocalTime lastEnd() {
        return segments.stream().map(ShiftSegment::getEnd).max(Comparator.naturalOrder()).orElseThrow();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public List<ShiftSegment> getSegments() {
        return segments;
    }
}
