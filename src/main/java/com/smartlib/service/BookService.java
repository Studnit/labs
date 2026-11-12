package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service

public class BookService {
    static final int MAX_ACTIVE_ISSUES = 5;

    private final BookRepository books;
    private final BookIssueRepository issues;
    private final AppUserRepository users;
    private final LibraryRepository libraries;
    private final PaymentService payments;
    private final FeatureService features;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUser ctx;


    public BookService(BookRepository books, BookIssueRepository issues, AppUserRepository users, LibraryRepository libraries, PaymentService payments, FeatureService features, NotificationService notifications, AuditService audit, CurrentUser ctx) {
        this.books = books;
        this.issues = issues;
        this.users = users;
        this.libraries = libraries;
        this.payments = payments;
        this.features = features;
        this.notifications = notifications;
        this.audit = audit;
        this.ctx = ctx;
    }
    @Transactional(readOnly = true)
    public List<Book> search(String q) { return books.search(ctx.requireLibraryId(), q == null ? "" : q.trim()); }
    @Transactional(readOnly = true)
    public Book get(Long id) { return books.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }

    @Transactional
    public Book save(Long id, String title, String author, String isbn, String category, String shelf, int copies) {
        if (copies < 0) throw new BusinessException("Copies cannot be negative");
        Book b;
        if (id == null) {
            b = new Book(); b.setLibrary(libraries.getReferenceById(ctx.requireLibraryId()));
            b.setTotalCopies(copies); b.setAvailableCopies(copies);
        } else {
            b = get(id);
            long out = b.getTotalCopies() - b.getAvailableCopies();
            if (copies < out) throw new BusinessException("Cannot set copies below the " + out + " currently issued");
            b.setAvailableCopies(copies - (int) out); b.setTotalCopies(copies);
        }
        b.setTitle(title.trim()); b.setAuthor(author); b.setIsbn(isbn); b.setCategory(category); b.setShelf(shelf);
        audit.log(id == null ? "BOOK_ADDED" : "BOOK_UPDATED", title);
        return books.save(b);
    }

    @Transactional
    public void delete(Long id) {
        Book b = get(id);
        if (issues.existsByBookId(id)) throw new BusinessException("This book has issue history; set copies to 0 instead of deleting");
        books.delete(b);
        audit.log("BOOK_DELETED", b.getTitle());
    }

    @Transactional
    public BookIssue issue(Long bookId, Long memberId, Integer days) {
        Long lib = ctx.requireLibraryId();
        Book b = get(bookId);
        AppUser m = users.findByIdAndLibraryId(memberId, lib).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (m.getRole() != Role.MEMBER || m.getStatus() != UserStatus.ACTIVE) throw new BusinessException("Select an active member");
        if (b.getAvailableCopies() < 1) throw new BusinessException("No copies available");
        if (issues.countByMemberIdAndReturnedAtIsNull(m.getId()) >= MAX_ACTIVE_ISSUES) throw new BusinessException("Member already has " + MAX_ACTIVE_ISSUES + " books issued");
        int d = days == null ? m.getLibrary().getMaxIssueDays() : days;
        if (d < 1 || d > 90) throw new BusinessException("Loan period must be 1-90 days");
        b.setAvailableCopies(b.getAvailableCopies() - 1);
        BookIssue i = new BookIssue();
        i.setLibrary(b.getLibrary()); i.setBook(b); i.setMember(m); i.setIssuedAt(LocalDate.now()); i.setDueDate(LocalDate.now().plusDays(d));
        i = issues.save(i);
        notifications.notify(m, "Book issued", b.getTitle() + " — due " + i.getDueDate(), "/books/mine");
        audit.log("BOOK_ISSUED", b.getTitle() + " -> " + m.getEmail());
        return i;
    }

    @Transactional
    public BookIssue returnBook(Long issueId) {
        BookIssue i = issues.findByIdAndLibraryId(issueId, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (i.getReturnedAt() != null) throw new BusinessException("Already returned");
        LocalDate today = LocalDate.now();
        i.setReturnedAt(today);
        i.getBook().setAvailableCopies(i.getBook().getAvailableCopies() + 1);
        long late = Math.max(0, ChronoUnit.DAYS.between(i.getDueDate(), today));
        if (late > 0) {
            BigDecimal fine = i.getLibrary().getFinePerDay().multiply(BigDecimal.valueOf(late));
            i.setFine(fine);
            if (fine.signum() > 0 && features.enabled(i.getLibrary().getId(), Feature.PAYMENTS))
                payments.createFine(i.getMember(), fine, "Late fine: " + i.getBook().getTitle() + " (" + late + " days)");
        }
        audit.log("BOOK_RETURNED", i.getBook().getTitle() + " <- " + i.getMember().getEmail() + (late > 0 ? " late " + late + "d" : ""));
        return i;
    }

    @Transactional(readOnly = true) public List<BookIssue> openIssues() { return issues.findByLibraryIdAndReturnedAtIsNullOrderByDueDate(ctx.requireLibraryId()); }
    @Transactional(readOnly = true) public List<BookIssue> myIssues() { return issues.findByMemberIdOrderByIssuedAtDesc(ctx.id()); }
}
