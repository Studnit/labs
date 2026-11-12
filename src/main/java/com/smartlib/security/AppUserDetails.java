package com.smartlib.security;

import com.smartlib.domain.AppUser;
import com.smartlib.domain.Role;
import com.smartlib.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Lightweight principal snapshot stored in the session (no JPA entities in the session). */
public class AppUserDetails implements UserDetails {
    private final Long id;
    private final Long libraryId;
    private final String email, fullName, passwordHash;
    private final Role role;
    private final UserStatus status;
    private final LocalDateTime lockedUntil;

    public AppUserDetails(AppUser u) {
        this.id = u.getId();
        this.libraryId = u.libraryId();
        this.email = u.getEmail();
        this.fullName = u.getFullName();
        this.passwordHash = u.getPasswordHash();
        this.role = u.getRole();
        this.status = u.getStatus();
        this.lockedUntil = u.getLockedUntil();
    }
    public Long getId() { return id; }
    public Long getLibraryId() { return libraryId; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonLocked() { return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now()); }
    @Override public boolean isEnabled() { return status == UserStatus.ACTIVE; }
}
