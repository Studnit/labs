package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "library")
public class Library extends BaseEntity {
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String slug;
    private String address;
    private String phone;
    private String email;
    private String upiId;
    @Column(length = 255) private String tagline;
    /** One facility per line, e.g. "Free Wi-Fi". */
    @Column(length = 1000) private String facilities;
    @Column(nullable = false, unique = true) private String qrToken = UUID.randomUUID().toString().replace("-", "");
    private boolean active = true;
    private LocalTime openTime = LocalTime.of(6, 0);
    private LocalTime closeTime = LocalTime.of(22, 0);
    @Column(precision = 10, scale = 2) private BigDecimal finePerDay = new BigDecimal("2.00");
    private int maxIssueDays = 14;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }
    public java.util.List<String> facilityList() {
        if (facilities == null || facilities.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(facilities.split("\\R")).map(String::strip).filter(x -> !x.isEmpty()).toList();
    }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }

    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }

    public BigDecimal getFinePerDay() { return finePerDay; }
    public void setFinePerDay(BigDecimal finePerDay) { this.finePerDay = finePerDay; }

    public int getMaxIssueDays() { return maxIssueDays; }
    public void setMaxIssueDays(int maxIssueDays) { this.maxIssueDays = maxIssueDays; }
}
