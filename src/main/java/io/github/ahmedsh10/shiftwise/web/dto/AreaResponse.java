package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.domain.Area;

public record AreaResponse(Long id, String code, String name) {

    public static AreaResponse from(Area a) {
        return new AreaResponse(a.getId(), a.getCode(), a.getName());
    }
}
