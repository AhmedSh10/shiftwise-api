package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.Site;
import io.github.ahmedsh10.shiftwise.repository.AreaRepository;
import io.github.ahmedsh10.shiftwise.repository.SiteRepository;
import io.github.ahmedsh10.shiftwise.web.dto.AreaResponse;
import io.github.ahmedsh10.shiftwise.web.dto.CreateAreaRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AreaService {

    private final AreaRepository areas;
    private final SiteRepository sites;

    public AreaService(AreaRepository areas, SiteRepository sites) {
        this.areas = areas;
        this.sites = sites;
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> list() {
        return areas.findAll(Sort.by("id")).stream().map(AreaResponse::from).toList();
    }

    /** Single-site for now: new areas belong to the first site. */
    @Transactional
    public AreaResponse create(CreateAreaRequest request) {
        if (areas.existsByCode(request.code())) {
            throw new ConflictException("Area code " + request.code() + " already exists");
        }
        Site site = sites.findAll(Sort.by("id")).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Site", "default"));
        return AreaResponse.from(areas.save(new Area(site, request.code(), request.name())));
    }
}
