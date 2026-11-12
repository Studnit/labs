package com.smartlib.feature;

import com.smartlib.domain.Feature;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central backend enforcement: every URL that belongs to a module is checked against the
 * caller's library settings. Pages AND JSON/REST endpoints of a module live under the same prefix,
 * so disabling a module closes all of them at once.
 */
@Component

public class FeatureInterceptor implements HandlerInterceptor {
    private static final Map<String, Feature> PREFIXES = new LinkedHashMap<>();
    static {
        PREFIXES.put("/attendance", Feature.QR_ATTENDANCE);
        PREFIXES.put("/subscriptions", Feature.SUBSCRIPTIONS);
        PREFIXES.put("/payments", Feature.PAYMENTS);
        PREFIXES.put("/seats", Feature.SEAT_BOOKING);
        PREFIXES.put("/books", Feature.BOOK_MANAGEMENT);
        PREFIXES.put("/feed", Feature.COMMUNITY_FEED);
        PREFIXES.put("/files/feed", Feature.COMMUNITY_FEED);
        PREFIXES.put("/forum", Feature.DISCUSSION_FORUM);
        PREFIXES.put("/chat", Feature.CHAT);
        PREFIXES.put("/groups", Feature.STUDY_GROUPS);
        PREFIXES.put("/files/groups", Feature.STUDY_GROUPS);
        PREFIXES.put("/guests", Feature.GUEST_MANAGEMENT);
        PREFIXES.put("/reports", Feature.REPORTS);
    }

    private final FeatureService features;
    private final CurrentUser ctx;
    private final LibraryRepository libraries;


    public FeatureInterceptor(FeatureService features, CurrentUser ctx, LibraryRepository libraries) {
        this.features = features;
        this.ctx = ctx;
        this.libraries = libraries;
    }
    public static Feature match(String path) {
        for (var e : PREFIXES.entrySet())
            if (path.equals(e.getKey()) || path.startsWith(e.getKey() + "/")) return e.getValue();
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        // Public guest registration: /g/{slug}
        if (path.startsWith("/g/")) {
            String slug = path.substring(3).split("/")[0];
            Long libId = libraries.findBySlug(slug).map(l -> l.getId()).orElse(null);
            if (libId == null || !features.enabled(libId, Feature.GUEST_MANAGEMENT))
                throw new FeatureDisabledException(Feature.GUEST_MANAGEMENT);
            return true;
        }
        Feature f = match(path);
        if (f == null) return true;
        if (!features.enabled(ctx.libraryId(), f)) throw new FeatureDisabledException(f);
        return true;
    }
}
