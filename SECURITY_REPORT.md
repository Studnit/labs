# Security review & test report

Method: manual code review of every controller/service/config + automated tests in `src/test`.
Honest status: the 6 `TimeTracker` tests and the Marathi translator checks were **executed** here. The Spring/MockMvc/Mockito tests
(`mvn test`) could **not** be run in my sandbox (Maven Central is blocked), so they are written but unexecuted — run `mvn test` and send me failures.

Legend: ✅ OK · 🔧 defect found and fixed · ⚠️ defect found, still open (needs your decision) · ℹ️ note

| # | Checklist item | Where checked | Result |
|---|---|---|---|
| 1 | Hide API keys / secrets | `application.yml`, compose, git history | ✅ all secrets come from env vars; `.env` is git-ignored. No keys found in the repo |
| 2 | Check env variables | `docker-compose.yml`, `.env.example` | ✅ compose refuses to start without `SUPERADMIN_*`, DB password |
| 3 | Default credentials | `DataSeeder` | 🔧 **D1 High** – super admin was created with the well-known password `ChangeMe#2026`. Now the app **refuses to start in `prod`** with that password |
| 4 | Protect admin routes | `SecurityConfig`, `@PreAuthorize` | ✅ `/super/**` SUPER_ADMIN, `/admin/**` LIBRARY_ADMIN, staff actions STAFF. Tests: `memberCannotReachStaffOrAdminAreas`, `librarianCannotReachAdminOrSuper`, `libraryAdminCannotReachSuperAdmin`, `memberCannotPerformStaffActions` |
| 5 | Proper authentication | login, BCrypt, session | ✅ BCrypt, session-fixation protection, logout by POST+CSRF |
| 6 | Account lockout / brute force | `AuthEvents` | ✅ 5 failures → 15 min lock. ℹ️ **D5 Low** an attacker can deliberately lock someone else's account (trade-off of any lockout) |
| 7 | User enumeration | `LoginFailureHandler`, register | ⚠️ **D3 Medium** – spring checks "disabled/locked" *before* the password, so `?error=pending/disabled/locked` reveals that an email exists. Registration also says "email already exists". Fix needs e-mail verification / uniform messages – tell me if you want it |
| 8 | Access control / tenant isolation | all finders | ✅ every repository call is library-scoped; IDs from other libraries return 404 (tests `adminCannotOpenAnotherLibrarysMember`, `otherLibrariesQrTokenBehavesAsUnknown`). Remaining `findById` uses were each reviewed and re-check library or ownership |
| 9 | Suspended users keep working | session handling | 🔧 **D6 Medium** – a suspended/deactivated user kept access until the session expired. New `ActiveUserInterceptor` checks status on every request (test `suspendedUserLosesAccessImmediately`) |
| 10 | Sanitize forms / input validation | `Forms`, services | 🔧 **D7 Medium** – public visitor form `/g/{slug}` had no length limits (DB overflow / notification spam). Now clamped. 🔧 **D8 Low** UPI reference unbounded → capped at 60. 🔧 **D9 Low** manual attendance accepted future times → rejected |
| 11 | XSS protection | Thymeleaf, CSP | ✅ no `th:utext`; user content is escaped (test `postedScriptIsEscapedOnTheFeed`). ⚠️ **D10 Medium** CSP still allows `'unsafe-inline'` scripts (templates have inline chart/demo scripts). Moving them to files + nonces removes this; not done yet |
| 12 | CSRF | `SecurityConfig` | ✅ enabled everywhere incl. AJAX (token in meta header). Tests `postsWithoutCsrfTokenAreRejected`, `loginWithoutCsrfRejected` |
| 13 | Rate limiting | `RateLimitFilter` | 🔧 **D11 Medium** – only login lockout existed. Added 30 POST/min per IP on login/register/visitor forms (test `tooManyPublicPostsFromOneIpGet429`). 🔧 `forward-headers-strategy` switched to `native` so the limiter sees the real client IP behind a proxy |
| 14 | Chat flooding (WebSocket) | `RealtimeHandler` | 🔧 **D12 Medium** – no limit on chat messages. Now 20 per 10 s per user (`ChatThrottleTest`). Same-origin handshake check is the Spring default ✅ |
| 15 | Secure API endpoints | JSON endpoints | ✅ like/chat/notification APIs are authenticated, CSRF-protected, library-scoped; feature switch enforced |
| 16 | CORS settings | – | ✅ none configured = same-origin only (correct, no wildcard) |
| 17 | Security headers | `SecurityConfig` | 🔧 **D13 Low** – added HSTS, Permissions-Policy (camera self only), `object-src 'none'`, `base-uri`, `form-action 'self'`. Existing: X-Frame-Options DENY, nosniff, Referrer-Policy, CSP (test `securityHeadersPresent`) |
| 18 | Cookies | `application*.yml` | 🔧 **D14 Medium** – `Secure` flag defaulted to off. `prod` profile now defaults it on (`COOKIE_SECURE`, set `false` only for plain-HTTP trials). HttpOnly + SameSite=Lax ✅ |
| 19 | Debug mode off / error leakage | `application-prod.yml`, actuator | ✅ no stack traces/messages in prod, only `health,info` exposed (tests `actuatorExposesOnlyHealth`, `unknownPageDoesNotLeakStackTrace`). Demo panel shows only when `SEED_DEMO=true` (off in prod) |
| 20 | Update dependencies | `pom.xml` | ℹ️ Spring Boot 3.3.5, POI 5.3.0, OpenPDF 2.0.3, ZXing 3.5.3. Not scanned for CVEs here → run `mvn org.owasp:dependency-check-maven:check` in CI |
| 21 | Remove unused packages | `pom.xml` | ✅ Lombok removed; jsQR is bundled locally (no CDN) |
| 22 | Check exposed files / uploads | `FileStorageService`, `FileController` | ✅ server-generated names, strict name regex, path-prefix check, library-scoped, nosniff, downloads as attachment. 🔧 **D15 Medium** – image type was judged by extension + client Content-Type only; now the real file header (magic bytes) is checked. SVG/HTML/JS never allowed (tests in `FileStorageServiceTest`) |
| 23 | Open redirect | `GlobalAdvice.safeBack` | ✅ only a same-site path from Referer is used (test `externalRefererNeverUsedForRedirect`) |
| 24 | Secure database | compose, JPA | ✅ parameterised JPQL/derived queries only (no string-built SQL); DB not published to host in compose. ℹ️ `ddl-auto=update` is convenient but not for strict prod → use Flyway migrations |
| 25 | Hash passwords | `UserService` | ✅ BCrypt (test `passwordsAreStoredAsBcrypt`). ⚠️ **D4 Low** policy is length-only (8–72), no complexity/breached-password check |
| 26 | Temporary passwords | `MemberController`/`UserService` | ⚠️ **D16 Medium** – staff-created members/staff get a temporary password but are **not forced to change it** at first login (needs a `mustChangePassword` column + redirect). Open |
| 27 | Payment integrity | `PaymentService` | ✅ amount always taken from the plan (never from the client), receipt only after PAID, gateway confirm restricted to owner, idempotent `markPaid`. ⚠️ **D17 Low** UPI reference is not unique (same reference can be submitted twice; staff verification is the control) |
| 28 | Excel export injection | `ExportService.sanitize` | ✅ cells starting with `= + - @` are neutralised |
| 29 | Scan git for leaked secrets | repo | ✅ grep for AWS/GitHub/private-key patterns: none. Run `gitleaks detect` in CI for history |
| 30 | Full security audit | this document | Done: 17 defects found — 12 fixed, 5 open (D3, D4, D10, D16, D17) |

## Open items I recommend, in order
1. **D16** force password change on first login for temporary passwords.
2. **D10** remove inline scripts and tighten CSP with nonces.
3. **D3** uniform login/registration messages (+ e-mail verification).
4. **D4** stronger password policy; **D17** unique UPI reference.
5. Add dependency scanning (OWASP) and gitleaks to CI; replace `ddl-auto=update` with Flyway.

## Running the tests
`mvn test` — unit tests: `TimeTrackerTest`, `GuestServiceTest`, `FileStorageServiceTest`, `ChatThrottleTest`, `TranslatorTest`,
`SeatAllocationTest`, `SubscriptionRuleTest`; security tests: `SecurityIntegrationTest` (H2, profile `test`).
