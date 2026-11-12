package com.smartlib.i18n;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Two languages only: English (default) and Marathi. The choice lives in the "sl-lang" cookie and can be changed with ?lang=en|mr.
 * Marathi pages are produced by translating the rendered HTML (see {@link Translator}).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LanguageFilter extends OncePerRequestFilter {
    public static final String COOKIE = "sl-lang";
    private final Translator translator;

    public LanguageFilter(Translator translator) { this.translator = translator; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        String p = r.getRequestURI().substring(r.getContextPath().length());
        return p.startsWith("/css/") || p.startsWith("/js/") || p.startsWith("/webjars/") || p.startsWith("/ws")
                || p.startsWith("/actuator") || p.startsWith("/files/") || p.startsWith("/img/") || p.equals("/favicon.ico")
                || "websocket".equalsIgnoreCase(r.getHeader("Upgrade"));
    }

    static String lang(HttpServletRequest req, HttpServletResponse res) {
        String q = req.getParameter("lang");
        if ("en".equals(q) || "mr".equals(q)) {
            Cookie c = new Cookie(COOKIE, q);
            c.setPath("/"); c.setMaxAge(60 * 60 * 24 * 365); c.setHttpOnly(true); c.setSecure(req.isSecure());
            res.addCookie(c);
            return q;
        }
        if (req.getCookies() != null) for (Cookie c : req.getCookies())
            if (COOKIE.equals(c.getName()) && "mr".equals(c.getValue())) return "mr";
        return "en";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String lang = lang(req, res);
        req.setAttribute("lang", lang);
        if (!"mr".equals(lang)) { chain.doFilter(req, res); return; }

        ContentCachingResponseWrapper w = new ContentCachingResponseWrapper(res);
        try {
            chain.doFilter(req, w);
        } finally {
            String ct = w.getContentType();
            byte[] body = w.getContentAsByteArray();
            if (ct != null && ct.toLowerCase().startsWith("text/html") && body.length > 0) {
                byte[] out = translator.html(new String(body, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                res.setContentLength(out.length);
                res.getOutputStream().write(out);
            } else {
                w.copyBodyToResponse();
            }
        }
    }
}
