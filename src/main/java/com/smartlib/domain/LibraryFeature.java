package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "library_feature", uniqueConstraints = @UniqueConstraint(columnNames = {"library_id", "feature"}))

public class LibraryFeature extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Feature feature;
    private boolean enabled = true;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public Feature getFeature() { return feature; }
    public void setFeature(Feature feature) { this.feature = feature; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
