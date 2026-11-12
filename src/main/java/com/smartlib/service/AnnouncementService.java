package com.smartlib.service;

import com.smartlib.domain.Announcement;
import com.smartlib.repo.AnnouncementRepository;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service

public class AnnouncementService {
    private final AnnouncementRepository repo;
    private final LibraryRepository libraries;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUser ctx;


    public AnnouncementService(AnnouncementRepository repo, LibraryRepository libraries, NotificationService notifications, AuditService audit, CurrentUser ctx) {
        this.repo = repo;
        this.libraries = libraries;
        this.notifications = notifications;
        this.audit = audit;
        this.ctx = ctx;
    }
    @Transactional(readOnly = true)
    public List<Announcement> list(int limit) { return repo.findByLibraryIdOrderByPinnedDescCreatedAtDesc(ctx.requireLibraryId(), PageRequest.of(0, limit)); }

    @Transactional
    public void post(String title, String body, boolean pinned) {
        if (title == null || title.isBlank() || body == null || body.isBlank()) throw new BusinessException("Title and message are required");
        Announcement a = new Announcement();
        a.setLibrary(libraries.getReferenceById(ctx.requireLibraryId())); a.setAuthor(ctx.user()); a.setTitle(title.trim()); a.setBody(body.trim()); a.setPinned(pinned);
        repo.save(a);
        notifications.notifyLibrary(a.getLibrary(), "Announcement: " + a.getTitle(), body.length() > 120 ? body.substring(0, 120) + "…" : body, "/announcements");
        audit.log("ANNOUNCEMENT_POSTED", title);
    }

    @Transactional
    public void delete(Long id) {
        Announcement a = repo.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repo.delete(a);
        audit.log("ANNOUNCEMENT_DELETED", a.getTitle());
    }
}
