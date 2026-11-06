package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;

import java.time.LocalTime;
import java.util.List;

public record ShiftTemplateResponse(Long id, String code, String name, List<Segment> segments, double hours) {

    public record Segment(LocalTime start, LocalTime end) {
    }

    public static ShiftTemplateResponse from(ShiftTemplate t) {
        List<Segment> segments = t.getSegments().stream().map(s -> new Segment(s.getStart(), s.getEnd())).toList();
        return new ShiftTemplateResponse(t.getId(), t.getCode(), t.getName(), segments,
                t.workedTime().toMinutes() / 60.0);
    }
}
