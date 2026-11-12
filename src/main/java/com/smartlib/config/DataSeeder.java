package com.smartlib.config;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Component

public class DataSeeder implements ApplicationRunner {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataSeeder.class);
    private final AppProperties props;
    private final AppUserRepository users;
    private final LibraryRepository libraries;
    private final SubscriptionPlanRepository plans;
    private final SeatRepository seats;
    private final BookRepository books;
    private final ForumCategoryRepository categories;
    private final FeatureService features;
    private final PasswordEncoder encoder;


    public DataSeeder(AppProperties props, AppUserRepository users, LibraryRepository libraries, SubscriptionPlanRepository plans, SeatRepository seats, BookRepository books, ForumCategoryRepository categories, FeatureService features, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.libraries = libraries;
        this.plans = plans;
        this.seats = seats;
        this.books = books;
        this.categories = categories;
        this.features = features;
        this.encoder = encoder;
    }
    static final String DEFAULT_SUPER_PASSWORD = "ChangeMe#2026";
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.core.env.Environment env;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (!users.existsByEmailIgnoreCase(props.superAdminEmail())) {
            boolean prod = java.util.Arrays.asList(env.getActiveProfiles()).contains("prod");
            if (prod && DEFAULT_SUPER_PASSWORD.equals(props.superAdminPassword()))
                throw new IllegalStateException("Refusing to start in prod with the default super-admin password. Set SUPERADMIN_PASSWORD.");
            AppUser s = user(null, "Super Admin", props.superAdminEmail(), props.superAdminPassword(), Role.SUPER_ADMIN);
            users.save(s);
            log.warn("Created super admin {} — change the password immediately (SUPERADMIN_PASSWORD).", s.getEmail());
        }
        if (props.seedDemo() && libraries.count() == 0) seedDemo();
        else seedLibraryFromEnv();
    }

    private AppUser user(Library lib, String name, String email, String pw, Role role) {
        AppUser u = new AppUser();
        u.setLibrary(lib); u.setFullName(name); u.setEmail(email.toLowerCase()); u.setPasswordHash(encoder.encode(pw));
        u.setRole(role); u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    /** The real library: Bhagat Singh Abhyasika (भगतसिंग अभ्यासिका), Nandurga, Tq. Ausa, Dist. Latur. */
    private Library createAbhyasika() {
        Library l = new Library();
        l.setName("Bhagat Singh Abhyasika"); l.setSlug("bhagatsingh");
        l.setAddress("Nandurga, Tq. Ausa, Dist. Latur");
        l.setTagline("An inspiring study library for quality studies");
        l.setFacilities(String.join("\n", "Calm and study-friendly environment", "Free Wi-Fi facility", "Reference books for reading",
                "Useful for competitive exam preparation", "Study facility during fixed hours"));
        l = libraries.save(l);
        features.initDefaults(l);
        return l;
    }

    /**
     * First start in production: create the library and its admin from LIBRARY_ADMIN_EMAIL / LIBRARY_ADMIN_PASSWORD
     * (skipped when either is empty or a library already exists). Seats and plans are then added from the admin screens.
     */
    private void seedLibraryFromEnv() {
        String email = env.getProperty("app.library-admin-email", ""), pw = env.getProperty("app.library-admin-password", "");
        if (email.isBlank() || pw.length() < 8 || libraries.count() > 0 || users.existsByEmailIgnoreCase(email)) return;
        Library l = createAbhyasika();
        users.save(user(l, "Library Admin", email.trim(), pw, Role.LIBRARY_ADMIN));
        log.warn("Created library '{}' with admin {}", l.getName(), email);
    }

    private void seedDemo() {
        Library l = createAbhyasika();
        users.save(user(l, "Asha Admin", "admin@abhyasika.local", "Admin#12345", Role.LIBRARY_ADMIN));
        users.save(user(l, "Leo Librarian", "librarian@abhyasika.local", "Libra#12345", Role.LIBRARIAN));
        for (int i = 1; i <= 3; i++) {
            AppUser m = user(l, "Member " + i, "member" + i + "@abhyasika.local", "Member#12345", Role.MEMBER);
            m = users.save(m);
            m.setMemberCode(String.format("L%d-M%05d", l.getId(), m.getId()));
        }
        String[][] p = {{"Monthly", "30", "500"}, {"Quarterly", "90", "1350"}, {"Day pass", "1", "30"}};
        for (String[] x : p) {
            SubscriptionPlan sp = new SubscriptionPlan(); sp.setLibrary(l); sp.setName(x[0]); sp.setDurationDays(Integer.parseInt(x[1])); sp.setPrice(new BigDecimal(x[2]));
            plans.save(sp);
        }
        for (int i = 1; i <= 20; i++) { Seat s = new Seat(); s.setLibrary(l); s.setLabel("A" + i); s.setZone("Silent hall"); seats.save(s); }
        String[][] b = {{"Clean Code", "Robert C. Martin", "Programming"}, {"Atomic Habits", "James Clear", "Self-help"}, {"Sapiens", "Yuval Noah Harari", "History"}};
        for (String[] x : b) { Book k = new Book(); k.setLibrary(l); k.setTitle(x[0]); k.setAuthor(x[1]); k.setCategory(x[2]); k.setTotalCopies(3); k.setAvailableCopies(3); books.save(k); }
        for (String n : new String[]{"General", "Exam preparation", "Book recommendations"}) {
            ForumCategory c = new ForumCategory(); c.setLibrary(l); c.setName(n); categories.save(c);
        }
        log.warn("Demo data created. Library admin: admin@abhyasika.local / Admin#12345  (disable with SEED_DEMO=false)");
    }
}
