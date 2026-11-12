package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "guest", indexes = @Index(columnList = "library_id"))
public class Guest extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @Column(nullable = false) private String name;
    private String phone;
    private String email;
    private String purpose;
    private String visiting;
    @Column(nullable = false) private LocalDateTime checkIn = LocalDateTime.now();
    private LocalDateTime checkOut;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getVisiting() { return visiting; }
    public void setVisiting(String visiting) { this.visiting = visiting; }

    public LocalDateTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDateTime checkIn) { this.checkIn = checkIn; }

    public LocalDateTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDateTime checkOut) { this.checkOut = checkOut; }
}
