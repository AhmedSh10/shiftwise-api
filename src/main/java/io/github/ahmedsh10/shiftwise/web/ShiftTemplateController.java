package io.github.ahmedsh10.shiftwise.web;

import io.github.ahmedsh10.shiftwise.service.ShiftTemplateService;
import io.github.ahmedsh10.shiftwise.web.dto.ShiftTemplateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shift-templates")
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
        this.shiftTemplateService = shiftTemplateService;
    }

    @GetMapping
    public List<ShiftTemplateResponse> list() {
        return shiftTemplateService.list();
    }
}
