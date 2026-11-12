package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "post_like", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class PostLike extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Post post;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser user;

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}
