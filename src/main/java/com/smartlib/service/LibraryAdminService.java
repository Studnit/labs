package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service

public class LibraryAdminService {
    private final LibraryRepository libraries;
    private final AppUserRepository users;
    private final FeatureService features;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final CurrentUser ctx;


    public LibraryAdminService(LibraryRepository libraries, AppUserRepository users, FeatureService features, PasswordEncoder encoder, AuditService audit, CurrentUser ctx) {
        this.libraries = libraries;
        this.users = users;
        this.features = features;
        this.encoder = encoder;
        this.audit = audit;
        this.ctx = ctx;
    }
    @Transactional(readOnly = true) public List<Library> all() { return libraries.findAll(); }

    /** Super admin: create a library together with its first Library Admin. */
    @Transactional
    public Library create(String name, String slug, String adminName, String adminEmail, String adminPassword) {
        String s = slug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        if (s.isBlank() || libraries.existsBySlug(s)) throw new BusinessException("Slug is empty or already used");
        if (users.existsByEmailIgnoreCase(adminEmail.trim())) throw new BusinessException("Admin email already registered");
        Library l = new Library(); l.setName(name.trim()); l.setSlug(s);
        l = libraries.save(l);
        features.initDefaults(l);
        AppUser a = new AppUser();
        a.setLibrary(l); a.setFullName(adminName.trim()); a.setEmail(adminEmail.trim().toLowerCase());
        a.setPasswordHash(encoder.encode(adminPassword)); a.setRole(Role.LIBRARY_ADMIN); a.setStatus(UserStatus.ACTIVE);
        users.save(a);
        audit.log(l, ctx.id(), ctx.details().getFullName(), "LIBRARY_CREATED", l.getName());
        return l;
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        Library l = libraries.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        l.setActive(active);
        audit.log(l, ctx.id(), ctx.details().getFullName(), active ? "LIBRARY_ACTIVATED" : "LIBRARY_DEACTIVATED", l.getName());
    }

    @Transactional
    public void updateSettings(String name, String address, String phone, String email, String upiId,
                               LocalTime open, LocalTime close, BigDecimal finePerDay, int maxIssueDays, String tagline, String facilities) {
        Library l = libraries.findById(ctx.requireLibraryId()).orElseThrow();
        if (!close.isAfter(open)) throw new BusinessException("Closing time must be after opening time");
        if (finePerDay.signum() < 0 || maxIssueDays < 1) throw new BusinessException("Invalid fine or loan period");
        l.setName(name.trim()); l.setAddress(address); l.setPhone(phone); l.setEmail(email); l.setUpiId(upiId);
        l.setTagline(tagline == null || tagline.isBlank() ? null : tagline.strip());
        l.setFacilities(facilities == null || facilities.isBlank() ? null : (facilities.length() > 1000 ? facilities.substring(0, 1000) : facilities.strip()));
        l.setOpenTime(open); l.setCloseTime(close); l.setFinePerDay(finePerDay); l.setMaxIssueDays(maxIssueDays);
        audit.log("LIBRARY_SETTINGS_UPDATED", l.getName());
    }

    @Transactional
    public void setFeature(Feature f, boolean enabled) {
        Library l = libraries.findById(ctx.requireLibraryId()).orElseThrow();
        features.set(l, f, enabled);
        audit.log("FEATURE_" + (enabled ? "ENABLED" : "DISABLED"), f.name());
    }

    @Transactional
    public void rotateQr() {
        Library l = libraries.findById(ctx.requireLibraryId()).orElseThrow();
        l.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        audit.log("QR_ROTATED", l.getName());
    }
}
