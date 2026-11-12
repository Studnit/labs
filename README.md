# Smart Library — Management, QR Attendance, Subscriptions & Student Community

Multi-library platform built with **Spring Boot 3.3 (Java 21), Thymeleaf, PostgreSQL, Spring Security, Bootstrap 5** and a small amount of vanilla JavaScript (WebSocket client, QR scanner, likes).

> **Status:** the code was written without access to Maven Central, so it has **not been compiled or run yet**.
> Run `mvn -q verify` first and expect to fix a handful of small compile/runtime issues. See "First run checklist" below.

## Quick start

```bash
# 1. database only
docker compose -f docker-compose.dev.yml up -d
# 2. app (dev profile defaults: demo data, mock payment gateway)
mvn spring-boot:run
# open http://localhost:8080
```

Demo accounts (only when `SEED_DEMO=true`, the default outside Docker prod):

| Role | Email | Password |
|---|---|---|
| Super Admin | `superadmin@smartlib.local` | `ChangeMe#2026` (set `SUPERADMIN_PASSWORD`) |
| Library Admin | `admin@abhyasika.local` | `Admin#12345` |
| Librarian | `librarian@abhyasika.local` | `Libra#12345` |
| Member | `member1@abhyasika.local` | `Member#12345` |

Production-style stack (app + PostgreSQL, persistent volumes):

```bash
cp .env.example .env     # edit secrets and APP_BASE_URL
docker compose up -d --build
```
Put a TLS-terminating reverse proxy (Caddy / nginx / Traefik) in front; it must forward `Upgrade` headers for `/ws`
and `X-Forwarded-*` headers. `APP_BASE_URL` is embedded in QR codes, so it must be the public HTTPS URL.

## Roles

| Role | Can do |
|---|---|
| **Super Admin** | Create/deactivate libraries and their first admin, platform audit log. No access to library data. |
| **Library Admin** | Everything a librarian can + module switches, library settings, staff accounts, subscription plans, QR regeneration, audit log. |
| **Librarian** | Approve members, manual attendance, sell/renew plans, record/verify payments, seats, books issue/return, guests, announcements, moderation, reports. |
| **Member** | QR check-in/out, own subscription & payments (UPI/online), seat booking, borrowed books, community modules. |
| **Guest** | Instant limited account (announcements, book catalogue). Visitors can also self-register without an account at `/g/{library-slug}` (QR shown in *Guests*). |

## Per-library module switches

Library Admin → **Modules** toggles: QR Attendance, Subscriptions, Payments, Seat Booking, Book Management,
Community Feed, Discussion Forum, Chat, Study Groups, Guest Management, Reports.

Disabled modules are removed in three layers:

1. **UI** – every nav link, dashboard tile and button is wrapped in `@features.on('…')`.
2. **HTTP/API** – `FeatureInterceptor` maps URL prefixes (pages, JSON endpoints, uploaded files) to a module and answers 403 (JSON for `/api/` calls) when it is off. Public visitor registration `/g/{slug}` is gated too.
3. **Services/WebSocket** – chat messages re-check the switch on every message; reports/exports, moderation and dashboard queries check it; scheduled reminders skip libraries with the module off.

Cross-module rules: subscriptions gate QR check-in and seat booking *only when Subscriptions is enabled*; late-return fines become pending payments *only when Payments is enabled*.

## Data isolation

Every tenant table has a `library_id`. The library is never taken from request parameters: it comes from the authenticated principal (`CurrentUser.requireLibraryId()`), and every repository lookup is `findByIdAndLibraryId(...)`. Objects from another library return 404. Uploaded files live under `uploads/{libraryId}/{module}/` and are served only for the caller's own library.

## Real-time

One WebSocket endpoint `/ws` (same-origin, session-authenticated) delivers notifications and chat. Messages are persisted first, then pushed to all room members.

## Security summary

BCrypt passwords, CSRF on all forms and fetch calls, session fixation protection, 5-failure/15-minute lockout, role checks on URLs and with `@PreAuthorize`, CSP and security headers, server-generated upload names with extension allow-lists, formula-injection-safe Excel export, redirect targets restricted to internal paths, audit log for sensitive actions.

## Payments

* **Cash / UPI (staff)** – recorded as paid immediately with a PDF receipt.
* **UPI (member)** – UPI QR for the library UPI ID; member submits the transaction reference; staff verify.
* **Online** – `PaymentGateway` interface. The bundled `MockPaymentGateway` always succeeds (only if `GATEWAY_MOCK=true`). **Implement a real provider (Razorpay, Stripe, …) as a `@Primary` bean before go-live**, including webhook verification.

## First run checklist

1. `mvn -q verify` (includes `FeatureInterceptorTest`) and fix any compile errors.
2. Start the app, sign in as each demo role, and click through every sidebar item.
3. Toggle a module off as Library Admin and confirm the nav item disappears and its URL returns 403.
4. Open two browsers for chat/notification real-time checks.

## Known limitations / next steps

* Schema is managed by Hibernate `ddl-auto=update`. For strict production change control, add Flyway migrations and set `DDL_AUTO=validate`.
* Feature cache (10 s TTL) and the WebSocket session registry are per-instance; scale out with sticky sessions or a broker (Redis/RabbitMQ).
* No email/SMS delivery yet; reminders are in-app notifications (hook points: `NotificationService`).
* No login/API rate limiting beyond account lockout – add at the proxy.
* Uploaded files are not virus-scanned.
* Time handling uses the server time zone; `TZ` defaults to `Asia/Kolkata` in `docker-compose.yml`.
* QR in-app scanner uses the browser `BarcodeDetector` API (Chrome/Android). Other browsers fall back to the phone camera app, which opens the same scan URL.

## Layout

```
src/main/java/com/smartlib
  config/    security, MVC, WebSocket, seeding, properties
  domain/    JPA entities + enums (Feature enum = module list)
  repo/      Spring Data repositories (tenant-scoped finders)
  security/  principal, CurrentUser, lockout events
  feature/   FeatureService, FeatureInterceptor
  service/   business logic (attendance, subscriptions, payments, seats, books, community, exports, jobs)
  web/       Thymeleaf controllers + exception handling
  ws/        WebSocket handler + session hub
src/main/resources/templates   Thymeleaf pages, fragments/layout, fragments/components
```

## Latest changes
- **Seat auto-allocation**: buying/renewing a subscription reserves a seat for the whole period — the member's previous seat if it is free, otherwise the first free seat. Seat selection needs an active subscription; staff can re-allocate from *Seats*. Members with a plan that has more than 7 days left cannot buy another one.
- **QR scanner for every browser**: live camera (BarcodeDetector, falling back to bundled jsQR), upload/take a photo, or paste the code/link. Live camera needs HTTPS (or localhost).
- **Colourful UI**: gradient theme, mobile bottom navigation, footer, and feature tiles for every enabled module.
- **Languages**: English and Marathi only. Switch in the top bar/footer (`?lang=mr`). Translations live in `src/main/resources/i18n/mr.tsv` (exact phrases) and `mr-patterns.tsv` (phrases with values); add a line to translate more. Untranslated text stays in English.
- **Security**: HSTS, Permissions-Policy, stricter CSP, per-IP rate limit on login/register/visitor forms, BCrypt, CSRF, account lockout. Behind a proxy the real client IP is used (forward-headers-strategy=native).

## Testing & security
See `SECURITY_REPORT.md` for the checklist, defects found/fixed and open items. Run `mvn test`.

## Your library: Bhagat Singh Abhyasika (भगतसिंग अभ्यासिका)
Nandurga, Tq. Ausa, Dist. Latur. The name, address, tagline and the five facilities from your poster are built in (shown in English or Marathi).
- **Production first start:** set `LIBRARY_ADMIN_EMAIL` and `LIBRARY_ADMIN_PASSWORD` (8+ chars) and the library plus its admin account are created automatically. Then log in and add seats (*Seats → Manage seats*), plans with your real prices (*Subscriptions*) and opening hours (*Library settings*).
- **Edit later:** *Library settings* lets the admin change the name, address, phone, UPI ID, tagline and facilities.
- Demo accounts (only when `SEED_DEMO=true`) now use `@abhyasika.local`.
# labs
