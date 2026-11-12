package com.smartlib.security;

import com.smartlib.domain.AppUser;
import com.smartlib.domain.Role;
import com.smartlib.repo.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Single access point for "who is calling and which library do they belong to". Used for tenant isolation. */
@Component("ctx")

public class CurrentUser {
    private final AppUserRepository users;


    public CurrentUser(AppUserRepository users) {
        this.users = users;
    }
    public AppUserDetails details() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof AppUserDetails d ? d : null;
    }
    public boolean authenticated() { return details() != null; }
    public Long id() { AppUserDetails d = details(); return d == null ? null : d.getId(); }
    public Long libraryId() { AppUserDetails d = details(); return d == null ? null : d.getLibraryId(); }
    public Role role() { AppUserDetails d = details(); return d == null ? null : d.getRole(); }
    public boolean staff() { Role r = role(); return r == Role.LIBRARY_ADMIN || r == Role.LIBRARIAN; }
    public boolean admin() { return role() == Role.LIBRARY_ADMIN; }
    public boolean superAdmin() { return role() == Role.SUPER_ADMIN; }

    public AppUser user() {
        Long id = id();
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    /** Library id of the caller; fails for users without a library (super admin). */
    public Long requireLibraryId() {
        Long l = libraryId();
        if (l == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No library context");
        return l;
    }
}
