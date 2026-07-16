-- V1: alap domain séma.
--
-- UUID elsődleges kulcsok minden, kliens felé is exponált entitáson: szándékosan NEM
-- sorszámozott bigint ID-kat használunk, mert azok triviálisan enumerálhatók
-- (pl. GET /api/bookings/124, /125, /126...) - ez klasszikus IDOR (Insecure Direct
-- Object Reference) vektor. UUID-vel az objektum ID önmagában nem ad ki információt
-- a rendszer méretéről, és nem lehet vele "végigsöpörni" az azonosítótartományt.
-- A gen_random_uuid() PostgreSQL 13 óta a core része, nem kell hozzá pgcrypto extension.

-- Generic trigger function: minden updated_at oszlopot automatikusan frissít UPDATE-kor.
-- Így ez adatbázis-szinten garantált, nem múlik azon, hogy minden service metódus
-- pontosan beállítja-e - védőháló, nem az egyetlen forrás az igazságra.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- app_user: minden bejelentkező fiók (vendég, szolgáltató, admin egyaránt).
-- ============================================================================
CREATE TABLE app_user (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                  VARCHAR(255) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    full_name              VARCHAR(255) NOT NULL,
    enabled                BOOLEAN NOT NULL DEFAULT true,
    account_locked         BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts  INT NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- app_role / user_role: szerepkörök, many-to-many a userrel.
-- Minden regisztráló user automatikusan ROLE_GUEST-et kap (lásd V2, seed adatok
-- alkalmazás oldali hozzárendelése). ROLE_PROVIDER csak admin jóváhagyás után kerül
-- hozzá (lásd provider.status), ROLE_ADMIN csak manuálisan/seeddel adható.
-- ============================================================================
CREATE TABLE app_role (
    id    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO app_role (name) VALUES ('ROLE_GUEST'), ('ROLE_PROVIDER'), ('ROLE_ADMIN');

CREATE TABLE user_role (
    user_id  UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id  INT  NOT NULL REFERENCES app_role (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================================
-- organization: a "szervezet/üzlet", amihez több szolgáltató (provider) tartozhat.
-- Amíg admin jóvá nem hagyja, PENDING állapotban van.
-- ============================================================================
CREATE TABLE organization (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED')),
    created_by_user_id  UUID NOT NULL REFERENCES app_user (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_organization_updated_at
    BEFORE UPDATE ON organization
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_organization_created_by ON organization (created_by_user_id);

-- ============================================================================
-- provider: egy user szolgáltatói "tagsága" egy szervezetben, admin jóváhagyási
-- állapottal. Ez a tábla a forrása a method-level security ownership-chain-nek:
-- TimeSlot -> ServiceOffering -> Provider -> User.
-- ============================================================================
CREATE TABLE provider (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL UNIQUE REFERENCES app_user (id) ON DELETE CASCADE,
    organization_id       UUID NOT NULL REFERENCES organization (id) ON DELETE CASCADE,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    applied_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at            TIMESTAMPTZ,
    decided_by_user_id    UUID REFERENCES app_user (id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_provider_updated_at
    BEFORE UPDATE ON provider
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_provider_organization ON provider (organization_id);
CREATE INDEX idx_provider_status ON provider (status);

-- ============================================================================
-- service_offering: a szolgáltató katalógustétele (pl. "Hajvágás - 30 perc").
-- ============================================================================
CREATE TABLE service_offering (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id       UUID NOT NULL REFERENCES provider (id) ON DELETE CASCADE,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    duration_minutes  INT NOT NULL CHECK (duration_minutes > 0),
    price             NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    active            BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_service_offering_updated_at
    BEFORE UPDATE ON service_offering
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_service_offering_provider ON service_offering (provider_id);

-- ============================================================================
-- time_slot: egy konkrét, foglalható időpont egy adott service_offering-hez.
-- Az átfedés-ellenőrzés (ugyanaz a provider ne hozhasson létre két egymást átfedő
-- slotot) V1-ben service-szinten, tranzakción belüli pesszimista lockkal történik -
-- ez itt még nincs DB constraint-tel kikényszerítve. Ez tudatos döntés: később,
-- amikor a haladó security/adatintegritás réteghez érünk, egy külön migrációban
-- hozzáadunk egy Postgres EXCLUDE constraint-et (btree_gist extension-nel) mint
-- DB-szintű védőhálót - így látszik a különbség "az alkalmazás validál" és
-- "az adatbázis fizikailag nem enged átfedést" között.
-- ============================================================================
CREATE TABLE time_slot (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_offering_id   UUID NOT NULL REFERENCES service_offering (id) ON DELETE CASCADE,
    start_time            TIMESTAMPTZ NOT NULL,
    end_time              TIMESTAMPTZ NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                              CHECK (status IN ('AVAILABLE', 'BOOKED', 'CANCELLED')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_time > start_time)
);

CREATE TRIGGER trg_time_slot_updated_at
    BEFORE UPDATE ON time_slot
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_time_slot_service_offering_start ON time_slot (service_offering_id, start_time);
CREATE INDEX idx_time_slot_status ON time_slot (status);

-- ============================================================================
-- booking: egy vendég foglalása egy time_slot-ra.
--
-- Nem hard UNIQUE(time_slot_id)-t használunk, mert azzal elveszne a foglalási
-- előzmény (lemondás után újrafoglalás ugyanarra a slotra). Ehelyett egy parciális
-- unique indexet teszünk: egy adott time_slot-hoz egyszerre legfeljebb egy CONFIRMED
-- állapotú booking tartozhat. Ez ugyanaz a réteges védelmi elv, mint a time_slot
-- átfedésnél: a service réteg is ellenőrzi ütközéskor, de ez itt a DB-szintű
-- végső biztosíték race condition (pl. két egyidejű foglalási kérés) ellen.
-- ============================================================================
CREATE TABLE booking (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    time_slot_id   UUID NOT NULL REFERENCES time_slot (id) ON DELETE RESTRICT,
    guest_user_id  UUID NOT NULL REFERENCES app_user (id),
    status         VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED'
                       CHECK (status IN ('CONFIRMED', 'CANCELLED_BY_GUEST', 'CANCELLED_BY_PROVIDER')),
    booked_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_booking_updated_at
    BEFORE UPDATE ON booking
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX uq_booking_confirmed_time_slot
    ON booking (time_slot_id)
    WHERE status = 'CONFIRMED';

CREATE INDEX idx_booking_guest ON booking (guest_user_id);

-- ============================================================================
-- refresh_token: JWT refresh token forgatáshoz. Csak a token HASH-e kerül tárolásra
-- (soha nem a nyers token), hogy egy DB-dump/leak esetén se legyen visszaélhető
-- token azonnal - ugyanaz az elv, mint a jelszavaknál.
-- ============================================================================
CREATE TABLE refresh_token (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash             VARCHAR(255) NOT NULL UNIQUE,
    expires_at             TIMESTAMPTZ NOT NULL,
    revoked                BOOLEAN NOT NULL DEFAULT false,
    revoked_at             TIMESTAMPTZ,
    replaced_by_token_id   UUID REFERENCES refresh_token (id),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

-- ============================================================================
-- audit_log: ki, mikor, mit módosított. actor_user_id ON DELETE SET NULL, mert
-- egy user törlése esetén is meg akarjuk tartani az audit bejegyzést (történeti
-- integritás fontosabb, mint a szigorú referenciális kapcsolat).
-- ============================================================================
CREATE TABLE audit_log (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id    UUID REFERENCES app_user (id) ON DELETE SET NULL,
    action           VARCHAR(50) NOT NULL,
    entity_type      VARCHAR(50) NOT NULL,
    entity_id        UUID,
    details          JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
