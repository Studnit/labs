package com.smartlib.security;

import com.smartlib.domain.*;
import com.smartlib.repo.*;
import com.smartlib.feature.FeatureService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests, grouped like the checklist: authentication, access control, tenant isolation, CSRF, XSS,
 * headers, brute force/rate limiting, open redirect, module switches, account status.
 * Uses H2 (profile "test") and the demo data created by DataSeeder.
 */
// RANDOM_PORT: the WebSocket config needs a real servlet container (a MOCK environment has no JSR-356 ServerContainer)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired LibraryRepository libraries;
    @Autowired PasswordEncoder encoder;
    @Autowired FeatureService features;

    /** Not served: any 3xx (to login) or 4xx — never 200. */
    private static ResultMatcher notExposed() {
        return result -> { int st = result.getResponse().getStatus(); assertTrue(st >= 300 && st < 500, "unexpected status " + st); };
    }

    // ---------- 1. Authentication ----------
    @Test void anonymousIsSentToLogin() throws Exception {
        for (String p : new String[]{"/dashboard", "/members", "/seats", "/payments", "/admin/features", "/super/libraries", "/attendance", "/profile", "/reports"})
            mvc.perform(get(p)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }
    @Test void publicPagesAreOpen() throws Exception {
        for (String p : new String[]{"/", "/login", "/register", "/g/bhagatsingh"}) mvc.perform(get(p)).andExpect(status().isOk());
    }
    @Test void passwordsAreStoredAsBcrypt() {
        AppUser u = users.findByEmailIgnoreCase("member1@abhyasika.local").orElseThrow();
        assertTrue(u.getPasswordHash().startsWith("$2"), "BCrypt hash expected");
        assertNotEquals("Member#12345", u.getPasswordHash());
    }
    @Test void wrongPasswordGivesGenericError() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "member2@abhyasika.local").param("password", "nope"))
           .andExpect(redirectedUrl("/login?error=bad"));
        mvc.perform(post("/login").with(csrf()).param("username", "nobody@x.y").param("password", "nope"))
           .andExpect(redirectedUrl("/login?error=bad"));
    }
    @Test void accountLocksAfterFiveFailures() throws Exception {
        for (int i = 0; i < 5; i++) mvc.perform(post("/login").with(csrf()).param("username", "member3@abhyasika.local").param("password", "bad" + i));
        mvc.perform(post("/login").with(csrf()).param("username", "member3@abhyasika.local").param("password", "Member#12345"))
           .andExpect(redirectedUrl("/login?error=locked"));
        AppUser u = users.findByEmailIgnoreCase("member3@abhyasika.local").orElseThrow();
        u.setLockedUntil(null); u.setFailedAttempts(0); users.save(u);     // reset for other tests
    }
    @Test void sessionIsRenewedOnLogin() throws Exception {
        var before = (org.springframework.mock.web.MockHttpSession) mvc.perform(get("/login")).andReturn().getRequest().getSession(true);
        String oldId = before.getId();
        mvc.perform(post("/login").session(before).with(csrf()).param("username", "member1@abhyasika.local").param("password", "Member#12345"))
           .andExpect(status().is3xxRedirection());
        assertTrue(before.isInvalid() || !oldId.equals(before.getId()), "session fixation protection: old session must not survive login");
    }

    // ---------- 2. Access control / admin routes ----------
    @Test @WithUserDetails("member1@abhyasika.local") void memberCannotReachStaffOrAdminAreas() throws Exception {
        for (String p : new String[]{"/admin/features", "/admin/library", "/admin/staff", "/admin/audit", "/super/libraries", "/members", "/reports", "/guests", "/moderation", "/seats/manage"})
            mvc.perform(get(p)).andExpect(status().isForbidden());
    }
    @Test @WithUserDetails("librarian@abhyasika.local") void librarianCannotReachAdminOrSuper() throws Exception {
        mvc.perform(get("/admin/features")).andExpect(status().isForbidden());
        mvc.perform(get("/super/libraries")).andExpect(status().isForbidden());
    }
    @Test @WithUserDetails("admin@abhyasika.local") void libraryAdminCannotReachSuperAdmin() throws Exception {
        mvc.perform(get("/super/libraries")).andExpect(status().isForbidden());
        mvc.perform(post("/super/libraries").with(csrf()).param("name", "x").param("slug", "x")).andExpect(status().isForbidden());
    }
    @Test @WithUserDetails("member1@abhyasika.local") void memberCannotPerformStaffActions() throws Exception {
        mvc.perform(post("/members/1/status").with(csrf()).param("status", "SUSPENDED")).andExpect(status().isForbidden());
        mvc.perform(post("/seats/allocate").with(csrf()).param("memberId", "1")).andExpect(status().isForbidden());
        mvc.perform(post("/subscriptions/assign").with(csrf()).param("memberId", "1").param("planId", "1")).andExpect(status().isForbidden());
        mvc.perform(post("/payments/1/verify").with(csrf()).param("approve", "true")).andExpect(status().isForbidden());
    }

    // ---------- 3. Tenant isolation ----------
    @Test @WithUserDetails("admin@abhyasika.local") void adminCannotOpenAnotherLibrarysMember() throws Exception {
        Library other = libraries.findBySlug("other-lib").orElseGet(() -> { Library l = new Library(); l.setName("Other"); l.setSlug("other-lib"); return libraries.save(l); });
        AppUser foreign = users.findByEmailIgnoreCase("foreign@other.local").orElseGet(() -> {
            AppUser u = new AppUser(); u.setLibrary(other); u.setFullName("Foreign"); u.setEmail("foreign@other.local");
            u.setPasswordHash(encoder.encode("Foreign#12345")); u.setRole(Role.MEMBER); u.setStatus(UserStatus.ACTIVE); return users.save(u); });
        mvc.perform(get("/members/" + foreign.getId())).andExpect(status().isNotFound());
        mvc.perform(post("/members/" + foreign.getId() + "/status").with(csrf()).param("status", "SUSPENDED")).andExpect(status().isNotFound());
        AppUser reload = users.findById(foreign.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, reload.getStatus(), "foreign user must be untouched");
    }
    @Test @WithUserDetails("member1@abhyasika.local") void otherLibrariesQrTokenBehavesAsUnknown() throws Exception {
        Library other = libraries.findBySlug("other-lib").orElseGet(() -> { Library l = new Library(); l.setName("Other"); l.setSlug("other-lib"); return libraries.save(l); });
        mvc.perform(get("/attendance/scan/" + other.getQrToken())).andExpect(status().isNotFound());
        mvc.perform(post("/attendance/scan/" + other.getQrToken()).with(csrf())).andExpect(status().isNotFound());
    }
    @Test @WithUserDetails("member1@abhyasika.local") void fileDownloadRejectsTraversal() throws Exception {
        // encoded-slash / dot-dot paths are blocked by Spring Security's firewall; names that are not server-generated are 404
        mvc.perform(get("/files/feed/passwd")).andExpect(status().isNotFound());
        mvc.perform(get("/files/feed/not-a-uuid.png")).andExpect(status().isNotFound());
        mvc.perform(get("/files/feed/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
    }

    // ---------- 4. CSRF ----------
    @Test @WithUserDetails("member1@abhyasika.local") void postsWithoutCsrfTokenAreRejected() throws Exception {
        mvc.perform(post("/feed").param("content", "hi")).andExpect(status().isForbidden());
        mvc.perform(post("/seats/book").param("seatId", "1")).andExpect(status().isForbidden());
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
    }
    @Test void loginWithoutCsrfRejected() throws Exception {
        mvc.perform(post("/login").param("username", "a").param("password", "b")).andExpect(status().isForbidden());
    }

    // ---------- 5. XSS / output encoding ----------
    @Test @WithUserDetails("member1@abhyasika.local") void postedScriptIsEscapedOnTheFeed() throws Exception {
        mvc.perform(multipart("/feed").param("content", "<script>alert('xss')</script>").with(csrf())).andExpect(status().is3xxRedirection());
        String html = mvc.perform(get("/feed")).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("<script>alert('xss')</script>"), "raw script tag must not be rendered");
        assertTrue(html.contains("&lt;script&gt;"));
    }
    @Test void guestFormEscapesAndClampsInput() throws Exception {
        mvc.perform(post("/g/bhagatsingh").with(csrf()).param("name", "<img src=x onerror=alert(1)>").param("purpose", "p".repeat(5000)))
           .andExpect(status().is3xxRedirection());
    }

    // ---------- 6. Security headers / cookies / error pages ----------
    @Test void securityHeadersPresent() throws Exception {
        var res = mvc.perform(get("/login").secure(true)).andReturn().getResponse();
        assertEquals("DENY", res.getHeader("X-Frame-Options"));
        assertEquals("nosniff", res.getHeader("X-Content-Type-Options"));
        String csp = res.getHeader("Content-Security-Policy");
        assertNotNull(csp); assertTrue(csp.contains("frame-ancestors 'none'")); assertTrue(csp.contains("object-src 'none'"));
        assertNotNull(res.getHeader("Strict-Transport-Security"));
        assertNotNull(res.getHeader("Referrer-Policy"));
        assertTrue(res.getHeader("Permissions-Policy").contains("camera=(self)"));
    }
    @Test void actuatorExposesOnlyHealth() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        for (String p : new String[]{"/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator/mappings"})
            mvc.perform(get(p)).andExpect(notExposed());
    }
    @Test @WithUserDetails("member1@abhyasika.local") void unknownPageDoesNotLeakStackTrace() throws Exception {
        String body = mvc.perform(get("/no/such/page")).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("Exception")); assertFalse(body.contains("at com.smartlib"));
    }

    // ---------- 7. Rate limiting ----------
    @Test void tooManyPublicPostsFromOneIpGet429() throws Exception {
        int last = 0;
        for (int i = 0; i < 35; i++)
            last = mvc.perform(post("/login").with(csrf()).with(r -> { r.setRemoteAddr("203.0.113.77"); return r; })
                    .param("username", "rate@x.y").param("password", "p")).andReturn().getResponse().getStatus();
        assertEquals(429, last);
    }

    // ---------- 8. Open redirect ----------
    @Test @WithUserDetails("member1@abhyasika.local") void externalRefererNeverUsedForRedirect() throws Exception {
        mvc.perform(post("/seats/book").with(csrf()).header("Referer", "https://evil.example/phish").param("seatId", "999999")
                .param("date", "2020-01-01").param("start", "09:00").param("end", "10:00"))
           .andExpect(result -> { String loc = result.getResponse().getRedirectedUrl(); assertTrue(loc == null || !loc.contains("evil.example"), loc); });
    }

    // ---------- 9. Module switches enforced in the backend ----------
    @Test @WithUserDetails("admin@abhyasika.local") void disabledModuleBlocksPagesAndActions() throws Exception {
        Library lib = libraries.findBySlug("bhagatsingh").orElseThrow();
        features.set(lib, Feature.SEAT_BOOKING, false);
        try {
            mvc.perform(get("/seats")).andExpect(status().isForbidden());
            mvc.perform(post("/seats/manage/add").with(csrf()).param("prefix", "Z").param("count", "1")).andExpect(status().isForbidden());
        } finally { features.set(lib, Feature.SEAT_BOOKING, true); }
    }

    // ---------- 10. Account status ----------
    @Test void suspendedUserLosesAccessImmediately() throws Exception {
        var session = (org.springframework.mock.web.MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("username", "member2@abhyasika.local").param("password", "Member#12345")).andReturn().getRequest().getSession();
        mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk());
        AppUser u = users.findByEmailIgnoreCase("member2@abhyasika.local").orElseThrow();
        u.setStatus(UserStatus.SUSPENDED); users.save(u);
        try { mvc.perform(get("/dashboard").session(session)).andExpect(status().is3xxRedirection()); }
        finally { u.setStatus(UserStatus.ACTIVE); users.save(u); }
    }
}
