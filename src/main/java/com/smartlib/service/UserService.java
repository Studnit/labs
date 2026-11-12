package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service

public class UserService {
    private final AppUserRepository users;
    private final LibraryRepository libraries;
    private final PasswordEncoder encoder;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUser ctx;


    public UserService(AppUserRepository users, LibraryRepository libraries, PasswordEncoder encoder, NotificationService notifications, AuditService audit, CurrentUser ctx) {
        this.users = users;
        this.libraries = libraries;
        this.encoder = encoder;
        this.notifications = notifications;
        this.audit = audit;
        this.ctx = ctx;
    }
    /** Public self-registration: members await approval, guests get instant limited access. */
    @Transactional
    public AppUser register(Long libraryId, String fullName, String email, String phone, String password, boolean asGuest) {
        Library lib = libraries.findById(libraryId).filter(Library::isActive).orElseThrow(() -> new BusinessException("Please choose a valid library"));
        if (users.existsByEmailIgnoreCase(email.trim())) throw new BusinessException("An account with this email already exists");
        AppUser u = new AppUser();
        u.setLibrary(lib); u.setFullName(fullName.trim()); u.setEmail(email.trim().toLowerCase()); u.setPhone(phone);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(asGuest ? Role.GUEST : Role.MEMBER);
        u.setStatus(asGuest ? UserStatus.ACTIVE : UserStatus.PENDING);
        u = users.save(u);
        if (!asGuest) {
            u.setMemberCode(String.format("L%d-M%05d", lib.getId(), u.getId()));
            notifications.notifyStaff(lib, "New member request", u.getFullName() + " is waiting for approval", "/members?status=PENDING");
        }
        audit.log(lib, u.getId(), u.getFullName(), asGuest ? "GUEST_ACCOUNT_REGISTERED" : "MEMBER_REGISTERED", u.getEmail());
        return u;
    }

    @Transactional
    public AppUser createStaff(String fullName, String email, String password, Role role) {
        if (role != Role.LIBRARIAN && role != Role.LIBRARY_ADMIN) throw new BusinessException("Invalid staff role");
        if (users.existsByEmailIgnoreCase(email.trim())) throw new BusinessException("An account with this email already exists");
        AppUser u = new AppUser();
        u.setLibrary(libraries.getReferenceById(ctx.requireLibraryId()));
        u.setFullName(fullName.trim()); u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(encoder.encode(password)); u.setRole(role); u.setStatus(UserStatus.ACTIVE);
        u = users.save(u);
        audit.log("STAFF_CREATED", role + ": " + u.getEmail());
        return u;
    }

    @Transactional(readOnly = true)
    public List<AppUser> list(String q, UserStatus status) {
        List<AppUser> all = users.search(ctx.requireLibraryId(), q == null ? "" : q.trim());
        return all.stream().filter(u -> u.getRole() != Role.SUPER_ADMIN && (status == null || u.getStatus() == status)).toList();
    }

    @Transactional(readOnly = true)
    public AppUser get(Long id) {
        return users.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void setStatus(Long id, UserStatus status) {
        AppUser u = get(id);
        if (u.getId().equals(ctx.id())) throw new BusinessException("You cannot change your own status");
        if (u.getRole() == Role.LIBRARY_ADMIN && !ctx.admin()) throw new BusinessException("Only a library admin can change an admin");
        UserStatus old = u.getStatus();
        u.setStatus(status);
        if (status == UserStatus.ACTIVE && u.getRole() == Role.MEMBER && u.getMemberCode() == null)
            u.setMemberCode(String.format("L%d-M%05d", u.getLibrary().getId(), u.getId()));
        users.save(u);
        audit.log("USER_STATUS", u.getEmail() + ": " + old + " -> " + status);
        if (status == UserStatus.ACTIVE && old == UserStatus.PENDING)
            notifications.notify(u, "Membership approved", "Welcome! Your membership has been approved.", "/dashboard");
    }

    @Transactional
    public void updateProfile(String fullName, String phone, String address, String bio) {
        AppUser u = ctx.user();
        u.setFullName(fullName.trim()); u.setPhone(phone); u.setAddress(address); u.setBio(bio);
        users.save(u);
    }

    @Transactional
    public void changePassword(String current, String next) {
        AppUser u = ctx.user();
        if (!encoder.matches(current, u.getPasswordHash())) throw new BusinessException("Current password is incorrect");
        u.setPasswordHash(encoder.encode(next));
        users.save(u);
        audit.log("PASSWORD_CHANGED", u.getEmail());
    }

    @Transactional(readOnly = true)
    public List<AppUser> activeMembers() {
        return users.findByLibraryIdAndRoleInAndStatus(ctx.requireLibraryId(), Set.of(Role.MEMBER), UserStatus.ACTIVE);
    }
}
