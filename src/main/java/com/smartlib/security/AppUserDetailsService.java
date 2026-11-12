package com.smartlib.security;

import com.smartlib.repo.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service

public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;


    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }
    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmailIgnoreCase(email.trim())
                .filter(u -> u.getLibrary() == null || u.getLibrary().isActive())
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
