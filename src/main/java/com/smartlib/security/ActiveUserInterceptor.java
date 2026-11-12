package com.smartlib.security;

import com.smartlib.domain.UserStatus;
import com.smartlib.repo.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** A suspended/deactivated user loses access immediately, not only at the next login. */
@Component
public class ActiveUserInterceptor implements HandlerInterceptor {
    private final AppUserRepository users;
    public ActiveUserInterceptor(AppUserRepository users) { this.users = users; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails d) {
            boolean active = users.statusOf(d.getId()).map(st -> st == UserStatus.ACTIVE).orElse(false);
            if (!active) {
                var session = req.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                res.sendRedirect(req.getContextPath() + "/login?error=disabled");
                return false;
            }
        }
        return true;
    }
}
