package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "forum_answer", indexes = @Index(columnList = "question_id"))
public class ForumAnswer extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private ForumQuestion question;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser author;
    @Column(length = 5000, nullable = false) private String body;
    private boolean accepted;
    private boolean hidden;

    public ForumQuestion getQuestion() { return question; }
    public void setQuestion(ForumQuestion question) { this.question = question; }

    public AppUser getAuthor() { return author; }
    public void setAuthor(AppUser author) { this.author = author; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
