package com.officemanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Column;
import javax.persistence.OneToMany;
import javax.persistence.FetchType;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "floors")
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "floor_seq")
    @SequenceGenerator(name = "floor_seq", sequenceName = "floor_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "floor_number")
    private Integer floorNumber;

    private String name;

    @OneToMany(mappedBy = "floor", fetch = FetchType.EAGER)
    @JsonIgnoreProperties("floor")
    private Set<OfficeRoom> rooms = new HashSet<>();

    // Default constructor required by JPA/Hibernate
    public Floor() {
    }

    public Floor(Long id, String name, Integer level) {
        this.id = id;
        this.name = name;
        this.floorNumber = level;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<OfficeRoom> getRooms() {
        return rooms;
    }

    public void setRooms(Set<OfficeRoom> rooms) {
        this.rooms = rooms;
    }
} 