package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "post", indexes = @Index(columnList = "library_id"))
public class Post extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser author;
    @Column(length = 4000, nullable = false) private String content;
    private String imageName;
    private boolean hidden;
    private boolean pinned;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public AppUser getAuthor() { return author; }
    public void setAuthor(AppUser author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}
