package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "forum_question", indexes = @Index(columnList = "library_id"))
public class ForumQuestion extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @ManyToOne(fetch = FetchType.LAZY) private ForumCategory category;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser author;
    @Column(nullable = false) private String title;
    @Column(length = 5000, nullable = false) private String body;
    private boolean resolved;
    private boolean hidden;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public ForumCategory getCategory() { return category; }
    public void setCategory(ForumCategory category) { this.category = category; }

    public AppUser getAuthor() { return author; }
    public void setAuthor(AppUser author) { this.author = author; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
