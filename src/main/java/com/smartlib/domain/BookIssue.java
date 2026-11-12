package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "book_issue", indexes = {@Index(columnList = "library_id"), @Index(columnList = "member_id")})
public class BookIssue extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Book book;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser member;
    @Column(nullable = false) private LocalDate issuedAt;
    @Column(nullable = false) private LocalDate dueDate;
    private LocalDate returnedAt;
    @Column(precision = 10, scale = 2) private BigDecimal fine = BigDecimal.ZERO;
    private boolean overdueNotified;
    public boolean isOverdue() { return returnedAt == null && dueDate.isBefore(LocalDate.now()); }

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public AppUser getMember() { return member; }
    public void setMember(AppUser member) { this.member = member; }

    public LocalDate getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDate issuedAt) { this.issuedAt = issuedAt; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDate returnedAt) { this.returnedAt = returnedAt; }

    public BigDecimal getFine() { return fine; }
    public void setFine(BigDecimal fine) { this.fine = fine; }

    public boolean isOverdueNotified() { return overdueNotified; }
    public void setOverdueNotified(boolean overdueNotified) { this.overdueNotified = overdueNotified; }
}
