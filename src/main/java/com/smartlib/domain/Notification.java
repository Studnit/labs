package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "notification", indexes = @Index(columnList = "user_id"))
public class Notification extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY) private Library library;
    @Column(nullable = false) private String title;
    @Column(length = 1000) private String message;
    private String link;
    private boolean seen;

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }
}
