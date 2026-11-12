package com.smartlib.service;

import com.smartlib.domain.Guest;
import com.smartlib.domain.Library;
import com.smartlib.repo.GuestRepository;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service

public class GuestService {
    private final GuestRepository guests;
    private final LibraryRepository libraries;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUser ctx;


    public GuestService(GuestRepository guests, LibraryRepository libraries, NotificationService notifications, AuditService audit, CurrentUser ctx) {
        this.guests = guests;
        this.libraries = libraries;
        this.notifications = notifications;
        this.audit = audit;
        this.ctx = ctx;
    }
    /** Public visitor self-registration via /g/{slug}. */
    @Transactional
    public Guest register(String slug, String name, String phone, String email, String purpose, String visiting) {
        Library lib = libraries.findBySlug(slug).filter(Library::isActive).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (name == null || name.isBlank()) throw new BusinessException("Name is required");
        Guest g = new Guest();
        g.setLibrary(lib); g.setName(clamp(name, 100)); g.setPhone(clamp(phone, 20)); g.setEmail(clamp(email, 150)); g.setPurpose(clamp(purpose, 200)); g.setVisiting(clamp(visiting, 100));
        g = guests.save(g);
        notifications.notifyStaff(lib, "Guest checked in", g.getName() + (purpose == null || purpose.isBlank() ? "" : " — " + purpose), "/guests");
        return g;
    }

    /** Public input is trimmed and cut to column size so a visitor cannot overflow the database or bloat notifications. */
    static String clamp(String v, int max) {
        if (v == null) return null;
        String t = v.strip();
        return t.length() > max ? t.substring(0, max) : t;
    }

    @Transactional
    public Guest addByStaff(String name, String phone, String purpose) {
        Guest g = new Guest();
        g.setLibrary(libraries.getReferenceById(ctx.requireLibraryId())); g.setName(clamp(name, 100)); g.setPhone(clamp(phone, 20)); g.setPurpose(clamp(purpose, 200));
        audit.log("GUEST_ADDED", name);
        return guests.save(g);
    }

    @Transactional
    public void checkOut(Long id) {
        Guest g = guests.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (g.getCheckOut() == null) g.setCheckOut(LocalDateTime.now());
    }

    @Transactional(readOnly = true) public List<Guest> recent() { return guests.findByLibraryIdOrderByCheckInDesc(ctx.requireLibraryId(), PageRequest.of(0, 200)); }
}
