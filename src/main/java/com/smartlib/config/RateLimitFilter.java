package com.smartlib.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple per-IP limit (30 POSTs/minute) on public form endpoints: login, register, visitor check-in. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 30;
    private static final long WINDOW_MS = 60_000;
    private final Map<String, long[]> hits = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        if (!"POST".equals(r.getMethod())) return true;
        String p = r.getRequestURI().substring(r.getContextPath().length());
        return !(p.equals("/login") || p.equals("/register") || p.startsWith("/g/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        if (hits.size() > 10_000) {
            for (Iterator<Map.Entry<String, long[]>> it = hits.entrySet().iterator(); it.hasNext(); )
                if (now - it.next().getValue()[0] > WINDOW_MS) it.remove();
        }
        long[] w = hits.compute(req.getRemoteAddr(), (k, v) -> {
            if (v == null || now - v[0] > WINDOW_MS) return new long[]{now, 1};
            v[1]++; return v;
        });
        if (w[1] > LIMIT) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write("Too many requests. Please wait a minute and try again.");
            return;
        }
        chain.doFilter(req, res);
    }
}
