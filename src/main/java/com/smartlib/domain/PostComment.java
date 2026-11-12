package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "post_comment", indexes = @Index(columnList = "post_id"))
public class PostComment extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Post post;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser author;
    @Column(length = 2000, nullable = false) private String content;
    private boolean hidden;

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public AppUser getAuthor() { return author; }
    public void setAuthor(AppUser author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
