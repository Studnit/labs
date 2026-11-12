package com.smartlib.web;

import com.smartlib.domain.Library;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.repo.NotificationRepository;
import com.smartlib.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Template helper ({@code @nav}) that works on every page, including error views. */
@Component("nav")

public class NavHelper {
    private final CurrentUser ctx;
    private final LibraryRepository libraries;
    private final NotificationRepository notifications;


    public NavHelper(CurrentUser ctx, LibraryRepository libraries, NotificationRepository notifications) {
        this.ctx = ctx;
        this.libraries = libraries;
        this.notifications = notifications;
    }
    public boolean active(String prefix) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a)) return false;
        HttpServletRequest r = a.getRequest();
        String path = r.getRequestURI().substring(r.getContextPath().length());
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
    public long unread() { Long id = ctx.id(); return id == null ? 0 : notifications.countByUserIdAndSeenFalse(id); }
    public String libraryName() {
        Long id = ctx.libraryId();
        return id == null ? "Smart Library" : libraries.findById(id).map(Library::getName).orElse("Smart Library");
    }
    public String userName() { var d = ctx.details(); return d == null ? "" : d.getFullName(); }
    public String roleLabel() { var r = ctx.role(); return r == null ? "" : r.name().replace('_', ' ').toLowerCase(); }
    public String hm(long minutes) { return com.smartlib.service.TimeTracker.hm(minutes); }
    public Library library() { Long id = ctx.libraryId(); return id == null ? null : libraries.findById(id).orElse(null); }
}
