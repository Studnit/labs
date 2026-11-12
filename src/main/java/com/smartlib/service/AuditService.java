package com.smartlib.service;

import com.smartlib.domain.AuditLog;
import com.smartlib.domain.Library;
import com.smartlib.repo.AuditLogRepository;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.AppUserDetails;
import com.smartlib.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service

public class AuditService {
    private final AuditLogRepository repo;
    private final LibraryRepository libraries;
    private final CurrentUser ctx;


    public AuditService(AuditLogRepository repo, LibraryRepository libraries, CurrentUser ctx) {
        this.repo = repo;
        this.libraries = libraries;
        this.ctx = ctx;
    }
    @Transactional
    public void log(String action, String details) {
        AppUserDetails d = ctx.details();
        write(d == null || d.getLibraryId() == null ? null : libraries.getReferenceById(d.getLibraryId()),
                d == null ? null : d.getId(), d == null ? "system" : d.getFullName(), action, details);
    }

    @Transactional
    public void log(Library library, Long actorId, String actorName, String action, String details) {
        write(library, actorId, actorName, action, details);
    }

    private void write(Library lib, Long actorId, String actorName, String action, String details) {
        AuditLog a = new AuditLog();
        a.setLibrary(lib); a.setActorId(actorId); a.setActorName(actorName); a.setAction(action);
        a.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            HttpServletRequest r = sra.getRequest();
            a.setIp(r.getRemoteAddr());
        }
        repo.save(a);
    }
}
