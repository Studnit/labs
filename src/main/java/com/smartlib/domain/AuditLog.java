package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "audit_log", indexes = @Index(columnList = "library_id"))
public class AuditLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) private Library library;
    private Long actorId;
    private String actorName;
    @Column(nullable = false) private String action;
    @Column(length = 1000) private String details;
    private String ip;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
