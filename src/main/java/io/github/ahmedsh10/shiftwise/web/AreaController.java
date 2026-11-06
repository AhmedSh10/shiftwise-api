package io.github.ahmedsh10.shiftwise.web;

import io.github.ahmedsh10.shiftwise.service.AreaService;
import io.github.ahmedsh10.shiftwise.web.dto.AreaResponse;
import io.github.ahmedsh10.shiftwise.web.dto.CreateAreaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping
    public List<AreaResponse> list() {
        return areaService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AreaResponse create(@Valid @RequestBody CreateAreaRequest request) {
        return areaService.create(request);
    }
}
