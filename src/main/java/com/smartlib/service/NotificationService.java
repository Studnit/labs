package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.repo.NotificationRepository;
import com.smartlib.ws.RealtimeHub;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service

public class NotificationService {
    private static final Set<Role> STAFF = Set.of(Role.LIBRARY_ADMIN, Role.LIBRARIAN);
    private static final Set<Role> EVERYONE = Set.of(Role.LIBRARY_ADMIN, Role.LIBRARIAN, Role.MEMBER, Role.GUEST);

    private final NotificationRepository repo;
    private final AppUserRepository users;
    private final RealtimeHub hub;


    public NotificationService(NotificationRepository repo, AppUserRepository users, RealtimeHub hub) {
        this.repo = repo;
        this.users = users;
        this.hub = hub;
    }
    @Transactional
    public void notify(AppUser user, String title, String message, String link) {
        Notification n = new Notification();
        n.setUser(user); n.setLibrary(user.getLibrary()); n.setTitle(title);
        n.setMessage(message == null ? "" : message); n.setLink(link);
        repo.save(n);
        hub.sendToUser(user.getId(), Map.of("type", "notification", "title", title,
                "message", message == null ? "" : message, "link", link == null ? "" : link,
                "unread", repo.countByUserIdAndSeenFalse(user.getId())));
    }

    @Transactional
    public void notifyStaff(Library lib, String title, String message, String link) {
        users.findByLibraryIdAndRoleInAndStatus(lib.getId(), STAFF, UserStatus.ACTIVE).forEach(u -> notify(u, title, message, link));
    }

    @Transactional
    public void notifyLibrary(Library lib, String title, String message, String link) {
        users.findByLibraryIdAndRoleInAndStatus(lib.getId(), EVERYONE, UserStatus.ACTIVE).forEach(u -> notify(u, title, message, link));
    }

    @Transactional(readOnly = true)
    public List<Notification> recent(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100)); }

    @Transactional
    public void markAllSeen(Long userId) { repo.markAllSeen(userId); }

    @Transactional
    public void markSeen(Long userId, Long id) {
        repo.findById(id).filter(n -> n.getUser().getId().equals(userId)).ifPresent(n -> { n.setSeen(true); repo.save(n); });
    }
}
