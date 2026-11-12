# Deploying

## Railway
1. New project → **Deploy from GitHub repo**. Add the **PostgreSQL** plugin to the same project.
2. In the app service → Variables (reference the database variables with `${{Postgres.PGHOST}}` etc.):
   - `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`  → from the Postgres service
   - `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD` → **required**, must not be `ChangeMe#2026` (app refuses to start in prod otherwise)
   - `LIBRARY_ADMIN_EMAIL`, `LIBRARY_ADMIN_PASSWORD` → creates *Bhagat Singh Abhyasika* and its admin on first start
   - `APP_BASE_URL` → your public URL, e.g. `https://yourapp.up.railway.app`
   - `RAILWAY_RUN_UID=0` → lets the app write to the mounted volume
   - Optional: `GATEWAY_MOCK=false` once a real payment gateway is wired, `SEED_DEMO=false` (default in prod)
3. Service → Settings → **Volumes** → add a volume mounted at `/app/uploads` (feed images, study materials).
4. Deploy. Health check: `/actuator/health/liveness` (already in `railway.json`).

`PORT` is injected by Railway and used automatically. HTTPS is terminated by Railway, so the secure cookie flag works.

## Any Docker host
`docker compose up -d --build` after copying `.env.example` to `.env` and filling it in.

## Not supported on Railway
The Dockerfile `VOLUME` instruction (removed). Use a Railway Volume instead.
