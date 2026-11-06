package io.github.ahmedsh10.shiftwise.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A place on the site that needs cleaning staff: wards, stairs, kitchen, ... */
@Entity
@Table(name = "areas")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    private String code;

    private String name;

    protected Area() {
    }

    public Area(Site site, String code, String name) {
        this.site = site;
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Site getSite() {
        return site;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
