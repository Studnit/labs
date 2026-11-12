package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "attendance", indexes = {@Index(columnList = "library_id"), @Index(columnList = "user_id")})

public class Attendance extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser user;
    @Column(nullable = false) private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    @Enumerated(EnumType.STRING) private AttendanceMethod method = AttendanceMethod.QR;
    private String note;
    private Long recordedBy;
    public Long minutes() { return Duration.between(checkIn, checkOut == null ? LocalDateTime.now() : checkOut).toMinutes(); }

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public LocalDateTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDateTime checkIn) { this.checkIn = checkIn; }

    public LocalDateTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDateTime checkOut) { this.checkOut = checkOut; }

    public AttendanceMethod getMethod() { return method; }
    public void setMethod(AttendanceMethod method) { this.method = method; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
}
