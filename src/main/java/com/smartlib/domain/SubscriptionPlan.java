package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "subscription_plan")
public class SubscriptionPlan extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @Column(nullable = false) private String name;
    private int durationDays = 30;
    @Column(precision = 10, scale = 2, nullable = false) private BigDecimal price = BigDecimal.ZERO;
    private boolean active = true;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
