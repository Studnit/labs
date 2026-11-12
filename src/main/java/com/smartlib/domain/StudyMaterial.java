package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "study_material")
public class StudyMaterial extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private StudyGroup group;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser uploader;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String storedName;
    private String originalName;
    private long sizeBytes;

    public StudyGroup getGroup() { return group; }
    public void setGroup(StudyGroup group) { this.group = group; }

    public AppUser getUploader() { return uploader; }
    public void setUploader(AppUser uploader) { this.uploader = uploader; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
}
