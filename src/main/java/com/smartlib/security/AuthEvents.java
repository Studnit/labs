package com.smartlib.security;

import com.smartlib.repo.AppUserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Brute-force protection: 5 bad passwords lock the account for 15 minutes. */
@Component

public class AuthEvents {
    static final int MAX_ATTEMPTS = 5;
    private final AppUserRepository users;


    public AuthEvents(AppUserRepository users) {
        this.users = users;
    }
    @EventListener @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent e) {
        users.findByEmailIgnoreCase(e.getAuthentication().getName()).ifPresent(u -> {
            u.setFailedAttempts(u.getFailedAttempts() + 1);
            if (u.getFailedAttempts() >= MAX_ATTEMPTS) {
                u.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                u.setFailedAttempts(0);
            }
            users.save(u);
        });
    }

    @EventListener @Transactional
    public void onSuccess(AuthenticationSuccessEvent e) {
        if (!(e.getAuthentication().getPrincipal() instanceof AppUserDetails d)) return;
        users.findById(d.getId()).ifPresent(u -> {
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
            u.setLastLoginAt(LocalDateTime.now());
            users.save(u);
        });
    }
}
