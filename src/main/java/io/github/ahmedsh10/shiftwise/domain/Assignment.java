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

import java.time.LocalDate;

/** One person working one shift in one area on one date. */
@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "area_id")
    private Area area;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shift_template_id")
    private ShiftTemplate shiftTemplate;

    @Column(name = "work_date")
    private LocalDate workDate;

    protected Assignment() {
    }

    public Assignment(Employee employee, Area area, ShiftTemplate shiftTemplate, LocalDate workDate) {
        this.employee = employee;
        this.area = area;
        this.shiftTemplate = shiftTemplate;
        this.workDate = workDate;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public Area getArea() {
        return area;
    }

    public ShiftTemplate getShiftTemplate() {
        return shiftTemplate;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }
}
