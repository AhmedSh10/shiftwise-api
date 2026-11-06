package io.github.ahmedsh10.shiftwise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** "The kitchen needs 2 people on the morning shift." */
@Entity
@Table(name = "coverage_requirements")
public class CoverageRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "area_id")
    private Area area;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shift_template_id")
    private ShiftTemplate shiftTemplate;

    @Column(name = "required_headcount")
    private int requiredHeadcount;

    protected CoverageRequirement() {
    }

    public CoverageRequirement(Area area, ShiftTemplate shiftTemplate, int requiredHeadcount) {
        this.area = area;
        this.shiftTemplate = shiftTemplate;
        this.requiredHeadcount = requiredHeadcount;
    }

    public Long getId() {
        return id;
    }

    public Area getArea() {
        return area;
    }

    public ShiftTemplate getShiftTemplate() {
        return shiftTemplate;
    }

    public int getRequiredHeadcount() {
        return requiredHeadcount;
    }

    public void setRequiredHeadcount(int requiredHeadcount) {
        this.requiredHeadcount = requiredHeadcount;
    }
}
