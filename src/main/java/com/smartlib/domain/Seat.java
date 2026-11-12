package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "seat", uniqueConstraints = @UniqueConstraint(columnNames = {"library_id", "label"}))
public class Seat extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @Column(nullable = false) private String label;
    private String zone;
    private boolean active = true;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
