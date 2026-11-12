package com.smartlib.security;

import com.smartlib.domain.UserStatus;
import com.smartlib.repo.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component

public class LoginFailureHandler implements AuthenticationFailureHandler {
    private final AppUserRepository users;


    public LoginFailureHandler(AppUserRepository users) {
        this.users = users;
    }
    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        String code = "bad";
        if (ex instanceof LockedException) code = "locked";
        else if (ex instanceof DisabledException) {
            String email = req.getParameter("username");
            code = email == null ? "disabled" : users.findByEmailIgnoreCase(email.trim())
                    .map(u -> u.getStatus() == UserStatus.PENDING ? "pending" : "disabled").orElse("disabled");
        }
        res.sendRedirect(req.getContextPath() + "/login?error=" + code);
    }
}
