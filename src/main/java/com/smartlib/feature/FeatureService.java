package com.smartlib.feature;

import com.smartlib.domain.Feature;
import com.smartlib.domain.Library;
import com.smartlib.domain.LibraryFeature;
import com.smartlib.repo.LibraryFeatureRepository;
import com.smartlib.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-library module switches. A module with no row is treated as enabled.
 * Exposed to Thymeleaf as {@code @features} so disabled modules vanish from the UI,
 * and enforced server-side by {@link FeatureInterceptor}, the WebSocket handler and services.
 */
@Service("features")

public class FeatureService {
    private static final long TTL_MS = 10_000;
    private record Entry(Set<Feature> enabled, long loadedAt) {}

    private final LibraryFeatureRepository repo;
    private final CurrentUser ctx;

    public FeatureService(LibraryFeatureRepository repo, CurrentUser ctx) {
        this.repo = repo;
        this.ctx = ctx;
    }
    private final Map<Long, Entry> cache = new ConcurrentHashMap<>();

    private Set<Feature> load(Long libraryId) {
        Entry e = cache.get(libraryId);
        if (e != null && System.currentTimeMillis() - e.loadedAt < TTL_MS) return e.enabled;
        Set<Feature> on = EnumSet.allOf(Feature.class);
        for (LibraryFeature lf : repo.findByLibraryId(libraryId)) if (!lf.isEnabled()) on.remove(lf.getFeature());
        cache.put(libraryId, new Entry(on, System.currentTimeMillis()));
        return on;
    }

    public boolean enabled(Long libraryId, Feature f) { return libraryId != null && load(libraryId).contains(f); }

    /** For templates: {@code th:if="${@features.on('CHAT')}"} */
    public boolean on(String name) { return enabled(ctx.libraryId(), Feature.valueOf(name)); }
    public boolean on(Feature f) { return enabled(ctx.libraryId(), f); }

    public void require(Long libraryId, Feature f) { if (!enabled(libraryId, f)) throw new FeatureDisabledException(f); }
    public void require(Feature f) { require(ctx.libraryId(), f); }

    public Map<Feature, Boolean> all(Long libraryId) {
        Set<Feature> on = load(libraryId);
        Map<Feature, Boolean> m = new LinkedHashMap<>();
        for (Feature f : Feature.values()) m.put(f, on.contains(f));
        return m;
    }

    @Transactional
    public void set(Library lib, Feature f, boolean enabled) {
        LibraryFeature lf = repo.findByLibraryIdAndFeature(lib.getId(), f).orElseGet(() -> {
            LibraryFeature n = new LibraryFeature(); n.setLibrary(lib); n.setFeature(f); return n; });
        lf.setEnabled(enabled);
        repo.save(lf);
        cache.remove(lib.getId());
    }

    @Transactional
    public void initDefaults(Library lib) {
        for (Feature f : Feature.values()) {
            if (repo.findByLibraryIdAndFeature(lib.getId(), f).isEmpty()) {
                LibraryFeature n = new LibraryFeature(); n.setLibrary(lib); n.setFeature(f); n.setEnabled(true); repo.save(n);
            }
        }
        cache.remove(lib.getId());
    }
}
