package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "content_report", indexes = @Index(columnList = "library_id,status"))
public class ContentReport extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser reporter;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportTarget targetType;
    @Column(nullable = false) private Long targetId;
    @Column(length = 500) private String reason;
    @Column(length = 500) private String snapshot;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportStatus status = ReportStatus.OPEN;
    private Long resolvedBy;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public AppUser getReporter() { return reporter; }
    public void setReporter(AppUser reporter) { this.reporter = reporter; }

    public ReportTarget getTargetType() { return targetType; }
    public void setTargetType(ReportTarget targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSnapshot() { return snapshot; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public Long getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Long resolvedBy) { this.resolvedBy = resolvedBy; }
}
