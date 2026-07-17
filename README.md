# Booking System

*Read this in: [English](#english) · [Magyar](#magyar)*

---

## English

> **A note on how this was built**
>
> This entire project — backend, frontend, architecture decisions, and this
> README itself — was built through AI-assisted ("vibecoded") pair-programming
> sessions with [Claude Code](https://claude.com/claude-code), Anthropic's AI
> coding agent. The repository owner directed the work at a high level
> (what to build, what to fix, what to prioritize, how it should look and
> feel) but did not personally write the code. Please don't take this
> repository as evidence of the owner's own hands-on coding ability — it's a
> demonstration of what's possible with an AI pair programmer, not a personal
> portfolio piece.

### What is this?

A small full-stack appointment booking platform. Guests can browse approved
service providers (a hairdresser, a dentist, a personal trainer, and so on),
view their services and available time slots, and book or cancel
appointments. Anyone can apply to become a provider; once an admin approves
the application, they get their own console to manage services and time
slots.

### Goal

This was a learning/practice project: an exercise in building a realistic
full-stack application end-to-end (Spring Boot 4 backend + Angular 22
frontend) with an AI coding agent, going beyond basic CRUD into things a real
application actually needs — JWT auth with refresh token rotation, rate
limiting, audit logging, ownership-based authorization, and a small but real
frontend with its own design system, including dark mode.

### What's included

**Backend** (Spring Boot 4.1, Java, PostgreSQL, Flyway)
- Registration/login with JWT access tokens
- DB-backed, hashed refresh tokens with rotation and reuse detection (a
  replayed/stolen refresh token revokes every active session for that user)
- Logout, and logout-everywhere
- Rate limiting on the login endpoint (brute-force protection)
- Role-based access control (guest / provider / admin) plus per-resource
  ownership checks (`@PreAuthorize` + a custom SpEL security bean)
- Provider application and admin approval workflow
- Service offering and time slot management, with overlap detection and
  row-level locking to prevent double-booking under concurrent requests
- Booking creation and cancellation
- An audit log of security- and business-relevant events

**Frontend** (Angular 22, standalone components + Signals)
- Guest flow: browse providers, book and cancel appointments
- Provider self-service console: apply, track application status, manage
  services and time slots
- Admin console: review and approve/reject provider applications
- Profile page and session management (log out / log out everywhere)
- Add booking to calendar (Google Calendar link or `.ics` download)
- AI booking assistant: a chat widget (Claude Haiku 4.5) that can search
  time slots, book, list, and cancel bookings on the user's behalf, scoped
  to that user's own permissions. Works without an API key configured (the
  rest of the app still runs; the chat endpoint just returns a clear error)
- Light and dark mode
- No UI framework — a small, hand-rolled design system (plain CSS/SCSS)

**Other**
- `scripts/seed-dummy-data.sh` — populates a running instance with
  realistic demo data (providers, services, time slots, bookings) through
  the real API, not raw SQL, so it behaves exactly like real usage would.

### Project structure

```
backend/    Spring Boot API (Java, Maven, PostgreSQL, Flyway)
frontend/   Angular app
scripts/    dev tooling (e.g. the demo data seed script)
```

### Running it locally

1. Start PostgreSQL and set the DB credentials the backend expects (see
   `backend/.env` / `application.yaml`).
2. `cd backend && ./mvnw spring-boot:run`
3. `cd frontend && npm install && npm start`
4. Optionally: `bash scripts/seed-dummy-data.sh` to fill it with demo data.

---

## Magyar

> **Egy megjegyzés arról, hogyan készült**
>
> Ez a teljes projekt — a backend, a frontend, az architektúra döntések, és
> maga ez a README is — AI-asszisztált, úgynevezett "vibecode"
> munkamenetekben készült a [Claude Code](https://claude.com/claude-code)-dal,
> az Anthropic AI kódoló ügynökével. A repó tulajdonosa magas szinten
> irányította a munkát (mit építsünk, mit javítsunk, mi legyen a prioritás,
> hogyan nézzen ki), de a kódot személyesen nem ő írta. Kérlek, ne tekintsd
> ezt a repót a tulajdonos saját kódolási képességének bizonyítékaként —
> inkább annak a bemutatója, mire képes egy AI pair-programmer, nem pedig
> egy személyes portfólió-darab.

### Miről szól?

Egy kisebb, teljes stack-es időpontfoglaló rendszer. A vendégek
böngészhetnek a jóváhagyott szolgáltatók között (fodrászat, fogászat,
személyi edző stb.), megnézhetik a szolgáltatásaikat és a szabad
időpontjaikat, majd foglalhatnak vagy lemondhatnak egy időpontot. Bárki
jelentkezhet szolgáltatónak; ha egy admin jóváhagyja a jelentkezését, saját
felületet kap a szolgáltatásai és időpontjai kezeléséhez.

### A cél

Ez egy tanuló-/gyakorló projekt volt: egy gyakorlat abból, hogyan épül fel
egy valóban működő, teljes stack-es alkalmazás elejétől a végéig (Spring
Boot 4 backend + Angular 22 frontend) egy AI kódoló ügynökkel, túlmutatva
az alap CRUD-on, olyan dolgokkal, amikre egy éles alkalmazásnak tényleg
szüksége van - JWT alapú autentikáció refresh token forgatással, rate
limiting, audit naplózás, tulajdonos-alapú jogosultságkezelés, és egy kicsi,
de valódi frontend saját design rendszerrel, sötét móddal együtt.

### Mi van benne?

**Backend** (Spring Boot 4.1, Java, PostgreSQL, Flyway)
- Regisztráció/bejelentkezés JWT access tokennel
- Adatbázisban tárolt, hash-elt refresh tokenek forgatással és
  újrafelhasználás-detektálással (ha valaki egy már felhasznált/ellopott
  refresh tokent próbál visszajátszani, az az adott user MINDEN aktív
  session-jét visszavonja)
- Kijelentkezés, illetve kijelentkezés minden eszközről
- Rate limiting a login endpointon (jelszó-brute-force elleni védelem)
- Szerepkör-alapú jogosultságkezelés (guest / provider / admin), plusz
  erőforrás-szintű tulajdonos-ellenőrzés (`@PreAuthorize` + egy saját SpEL
  security bean)
- Provider-jelentkezés és admin jóváhagyási folyamat
- Szolgáltatás- és időpont-kezelés, átfedés-ellenőrzéssel és sor-szintű
  zárolással, hogy egyidejű kérések esetén se lehessen dupla-foglalás
- Foglalás létrehozása és lemondása
- Audit napló a biztonsági és üzleti szempontból releváns eseményekről

**Frontend** (Angular 22, standalone komponensek + Signals)
- Vendég folyamat: providerek böngészése, időpont foglalása és lemondása
- Provider saját felület: jelentkezés, a jelentkezés állapotának követése,
  szolgáltatások és időpontok kezelése
- Admin felület: provider-jelentkezések elbírálása (jóváhagyás/elutasítás)
- Profil oldal és munkamenet-kezelés (kijelentkezés / kijelentkezés
  mindenhonnan)
- Foglalás hozzáadása a naptárhoz (Google Calendar link vagy `.ics` letöltés)
- AI foglalási asszisztens: egy chat widget (Claude Haiku 4.5), ami a
  felhasználó nevében tud időpontot keresni, foglalni, listázni és lemondani
  - kizárólag az adott felhasználó saját jogosultságain belül. API kulcs
  nélkül is működik minden más (a chat endpoint ilyenkor egyszerűen egy
  érthető hibaüzenetet ad)
- Világos és sötét mód
- Nincs UI keretrendszer - egy kicsi, kézzel épített design rendszer
  (sima CSS/SCSS)

**Egyéb**
- `scripts/seed-dummy-data.sh` - egy futó példányt tölt fel realisztikus
  demo adatokkal (providerek, szolgáltatások, időpontok, foglalások) a
  valódi API-n keresztül, nem nyers SQL-lel, így pontosan úgy viselkedik,
  mint egy valódi használat.

### Projekt-struktúra

```
backend/    Spring Boot API (Java, Maven, PostgreSQL, Flyway)
frontend/   Angular alkalmazás
scripts/    fejlesztői eszközök (pl. a demo adat seed script)
```

### Helyi futtatás

1. Indítsd el a PostgreSQL-t, és állítsd be a backend által várt DB
   hitelesítő adatokat (lásd `backend/.env` / `application.yaml`).
2. `cd backend && ./mvnw spring-boot:run`
3. `cd frontend && npm install && npm start`
4. Opcionálisan: `bash scripts/seed-dummy-data.sh` a demo adatok
   feltöltéséhez.
