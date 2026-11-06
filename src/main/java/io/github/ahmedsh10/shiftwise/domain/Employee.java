package io.github.ahmedsh10.shiftwise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "contract_hours_per_week")
    private int contractHoursPerWeek;

    private boolean active = true;

    /** Areas this person is trained to work in. */
    @ManyToMany
    @JoinTable(name = "employee_areas",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "area_id"))
    private Set<Area> skills = new HashSet<>();

    protected Employee() {
    }

    public Employee(String fullName, String email, String passwordHash, Role role, int contractHoursPerWeek) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.contractHoursPerWeek = contractHoursPerWeek;
    }

    public boolean canWorkIn(Area area) {
        return skills.stream().anyMatch(a -> a.getId().equals(area.getId()));
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public int getContractHoursPerWeek() {
        return contractHoursPerWeek;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Area> getSkills() {
        return skills;
    }

    public void setSkills(Set<Area> skills) {
        this.skills = skills;
    }
}
