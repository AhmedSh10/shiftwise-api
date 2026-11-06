package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.repository.ShiftTemplateRepository;
import io.github.ahmedsh10.shiftwise.web.dto.ShiftTemplateResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShiftTemplateService {

    private final ShiftTemplateRepository templates;

    public ShiftTemplateService(ShiftTemplateRepository templates) {
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    public List<ShiftTemplateResponse> list() {
        return templates.findAll(Sort.by("id")).stream().map(ShiftTemplateResponse::from).toList();
    }
}
