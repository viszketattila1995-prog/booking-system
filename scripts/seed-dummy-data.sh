#!/usr/bin/env bash
# Feltölti a futó backendet (alapból http://localhost:8080) realisztikus demo
# adatokkal: néhány jóváhagyott provider, szolgáltatásokkal és időpontokkal,
# plusz pár guest user, akik közül néhányan már foglaltak is.
#
# Szándékosan a VALÓDI REST API-n keresztül dolgozik (curl), nem nyers SQL
# INSERT-ekkel - így a jelszavak helyesen bcrypt-elve kerülnek be, a provider
# jóváhagyás átmegy a tényleges admin workflow-n (audit log bejegyzéssel
# együtt), a foglalások pedig a valódi zárolási/ütközésvizsgálati logikán.
#
# A /auth/login rate-limitelt (5 kérés/60mp/IP - lásd LoginRateLimitFilter),
# ezért a script tudatosan KERÜLI a login hívásokat, ahol lehet: friss
# regisztráció után a kapott refresh tokennel /auth/refresh-el szerez új
# (immár ROLE_PROVIDER-t is tartalmazó) access tokent a jóváhagyás után,
# ami NEM rate-limitelt. Login hívásra csak akkor van szükség, ha egy user
# már létezik egy korábbi futtatásból (409 a regisztrációnál) - ha ilyenkor
# 429-et kapunk, a script megvárja az ablak resetjét és újrapróbálja.
#
# Újra futtatható: a fix e-mail című userek regisztrációja 409-et ad
# másodjára (ilyenkor bejelentkezik rájuk), a service offering létrehozás
# névre duplikáció-ellenőrzött, az időpontok viszont a futtatás pillanatához
# képest relatívak, így minden futtatás friss, jövőbeli időpontokat ad hozzá.

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
ADMIN_EMAIL="admin@booking-system.local"
ADMIN_PASSWORD="Admin123!"
SEED_PASSWORD="Passw0rd!123"

log() { echo "[seed] $*" >&2; }

# Hibatűrő: hiányzó kulcsnál üres stringet ad vissza ahelyett, hogy elhasalna
# (KeyError = nem-nulla exit = "set -e" miatt az egész script megállna) - egy
# átmeneti hiba (pl. egy rate-limitelt login) ne vigye el az egész seedelést.
json_get() {
  python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d$1)
except Exception:
    print('')
"
}

# rate_limited_login EMAIL PASSWORD -> stdout: full JSON response
# A login végpont rate-limitelt (lásd LoginRateLimitFilter, 5 kérés/60mp/IP).
# Előre kitalálni, mikor kell szüneteltetni, törékeny (más hívások, más
# időzítés a script korábbi futásaitól függően) - ehelyett egyszerűen
# reagálunk a TÉNYLEGES 429 válaszra, és megvárjuk az ablak resetjét.
rate_limited_login() {
  local email="$1" password="$2"
  local attempt resp status body
  for attempt in 1 2 3; do
    resp=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/auth/login" -H 'Content-Type: application/json' \
      -d "{\"email\":\"$email\",\"password\":\"$password\"}")
    status=$(echo "$resp" | tail -1)
    body=$(echo "$resp" | sed '$d')
    if [ "$status" != "429" ]; then
      echo "$body"
      return
    fi
    log "  (login rate-limited, waiting 65s before retry $attempt/3...)"
    sleep 65
  done
  echo "$body"
}

# register_or_login EMAIL FULLNAME -> stdout: "accessToken refreshToken"
register_or_login() {
  local email="$1" fullName="$2"
  local resp status body

  resp=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$SEED_PASSWORD\",\"fullName\":\"$fullName\"}")
  status=$(echo "$resp" | tail -1)
  body=$(echo "$resp" | sed '$d')

  if [ "$status" != "201" ]; then
    body=$(rate_limited_login "$email" "$SEED_PASSWORD")
  fi

  echo "$body" | json_get "['accessToken']"
  echo "$body" | json_get "['refreshToken']"
}

# ensure_approved_provider EMAIL FULLNAME ORG_NAME ORG_DESC ADMIN_TOKEN -> stdout: provider access token (with ROLE_PROVIDER)
ensure_approved_provider() {
  local email="$1" fullName="$2" orgName="$3" orgDesc="$4" adminToken="$5"
  local tokens accessToken refreshToken

  tokens=$(register_or_login "$email" "$fullName")
  accessToken=$(echo "$tokens" | sed -n '1p')
  refreshToken=$(echo "$tokens" | sed -n '2p')

  local status_resp status body provider_status
  status_resp=$(curl -s -w '\n%{http_code}' "$BASE_URL/providers/me" -H "Authorization: Bearer $accessToken")
  status=$(echo "$status_resp" | tail -1)
  body=$(echo "$status_resp" | sed '$d')

  if [ "$status" = "204" ]; then
    log "  applying as provider: $orgName"
    curl -s -X POST "$BASE_URL/providers/apply" \
      -H "Authorization: Bearer $accessToken" -H 'Content-Type: application/json' \
      -d "{\"organizationName\":\"$orgName\",\"organizationDescription\":\"$orgDesc\"}" > /dev/null
    provider_status="PENDING"
  else
    provider_status=$(echo "$body" | json_get "['status']")
  fi

  if [ "$provider_status" = "PENDING" ]; then
    local provider_id
    provider_id=$(curl -s "$BASE_URL/providers/me" -H "Authorization: Bearer $accessToken" | json_get "['id']")
    log "  admin approving: $orgName"
    curl -s -X POST "$BASE_URL/admin/providers/$provider_id/approve" -H "Authorization: Bearer $adminToken" > /dev/null
    # Friss JWT kell, hogy tartalmazza a ROLE_PROVIDER-t - /auth/refresh-fel
    # szerezzük, NEM /auth/login-nal, mert az utóbbi rate-limitelt.
    accessToken=$(curl -s -X POST "$BASE_URL/auth/refresh" -H 'Content-Type: application/json' \
      -d "{\"refreshToken\":\"$refreshToken\"}" | json_get "['accessToken']")
  elif [ "$provider_status" != "APPROVED" ]; then
    log "  WARNING: $orgName provider status is $provider_status, skipping service setup"
    echo ""
    return
  fi

  echo "$accessToken"
}

# ensure_service_offering PROVIDER_TOKEN NAME DESC DURATION_MIN PRICE -> stdout: offering id
ensure_service_offering() {
  local token="$1" name="$2" desc="$3" duration="$4" price="$5"
  local existing_id
  existing_id=$(curl -s "$BASE_URL/providers/me/service-offerings" -H "Authorization: Bearer $token" \
    | python3 -c "import json,sys; d=json.load(sys.stdin); m=[o for o in d if o['name']=='$name']; print(m[0]['id'] if m else '')")

  if [ -n "$existing_id" ]; then
    echo "$existing_id"
    return
  fi

  curl -s -X POST "$BASE_URL/providers/me/service-offerings" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -d "{\"name\":\"$name\",\"description\":\"$desc\",\"durationMinutes\":$duration,\"price\":$price}" \
    | json_get "['id']"
}

# add_time_slot PROVIDER_TOKEN OFFERING_ID DAYS_FROM_NOW HOUR DURATION_MIN
add_time_slot() {
  local token="$1" offeringId="$2" daysFromNow="$3" hour="$4" durationMin="$5"
  local times start end
  # start és end EGY python-hívásból jön, end = start + duration - korábban két
  # külön hívás számolta őket, és a második .replace(hour=..., minute=0, ...)
  # nullázta vissza a duration hozzáadásával kapott percet, így start == end
  # lett szinte mindig (a backend meg elutasította, mert endTime nem "after"
  # startTime - ezért tűnt el csendben a legtöbb időpont).
  times=$(python3 -c "
import datetime
start = (datetime.datetime.now(datetime.UTC) + datetime.timedelta(days=$daysFromNow)).replace(hour=$hour, minute=0, second=0, microsecond=0)
end = start + datetime.timedelta(minutes=$durationMin)
print(start.isoformat().replace('+00:00', 'Z'))
print(end.isoformat().replace('+00:00', 'Z'))
")
  start=$(echo "$times" | sed -n '1p')
  end=$(echo "$times" | sed -n '2p')

  curl -s -X POST "$BASE_URL/providers/me/service-offerings/$offeringId/time-slots" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -d "{\"startTime\":\"$start\",\"endTime\":\"$end\"}" > /dev/null
}

log "waiting for backend at $BASE_URL ..."
# Szándékosan NEM /auth/login-t pollozzuk itt (az rate-limitelt, és -f nélkül/
# -f-fel is minden 4xx válasz újabb próbálkozásnak, azaz újabb login-kísérletnek
# számítana) - a /auth/register-t hívjuk üres body-val, ami mindig 400-at ad,
# de attól még bizonyítja, hogy a szerver válaszol. curl -f NÉLKÜL bármelyik
# HTTP válasz (státusztól függetlenül) sikeres curl exit code-ot ad - csak
# kapcsolódási hiba esetén tér vissza nem-nulla kóddal.
for i in $(seq 1 30); do
  if curl -s -o /dev/null "$BASE_URL/auth/register" -X POST -H 'Content-Type: application/json' -d '{}'; then
    break
  fi
  sleep 1
done

log "logging in as admin"
ADMIN_TOKEN=$(rate_limited_login "$ADMIN_EMAIL" "$ADMIN_PASSWORD" | json_get "['accessToken']")

# ---------------------------------------------------------------------------
# Providers + their services + a spread of future time slots
# ---------------------------------------------------------------------------

log "provider: Zold Furt Fodraszat"
T1=$(ensure_approved_provider "hairdresser@example.com" "Nagy Eszter" "Zöld Fürt Fodrászat" "Modern hajvágás és festés a belvárosban." "$ADMIN_TOKEN")
if [ -n "$T1" ]; then
  O1=$(ensure_service_offering "$T1" "Női hajvágás" "Mosás, vágás, szárítás." 60 8500)
  O2=$(ensure_service_offering "$T1" "Férfi hajvágás" "Gyors, precíz férfi vágás." 30 4500)
  O3=$(ensure_service_offering "$T1" "Hajfestés" "Tő- vagy teljes festés, tanácsadással." 120 18000)
  for d in 1 2 3 5 7; do add_time_slot "$T1" "$O1" "$d" 9 60; done
  for d in 1 2 4 6; do add_time_slot "$T1" "$O2" "$d" 11 30; done
  for d in 3 8; do add_time_slot "$T1" "$O3" "$d" 13 120; done
fi

log "provider: Mosoly Fogaszat"
T2=$(ensure_approved_provider "dentist@example.com" "Dr. Kovács Péter" "Mosoly Fogászat" "Fájdalommentes fogászat, korszerű technikával." "$ADMIN_TOKEN")
if [ -n "$T2" ]; then
  O4=$(ensure_service_offering "$T2" "Fogászati szűrés" "Éves kontroll, státuszfelmérés." 30 6000)
  O5=$(ensure_service_offering "$T2" "Fogkő eltávolítás" "Ultrahangos fogkőeltávolítás polírozással." 45 12000)
  O6=$(ensure_service_offering "$T2" "Fogfehérítés" "Professzionális, kíméletes fehérítés." 60 35000)
  for d in 2 4 6 9; do add_time_slot "$T2" "$O4" "$d" 8 30; done
  for d in 1 5; do add_time_slot "$T2" "$O5" "$d" 10 45; done
  for d in 7; do add_time_slot "$T2" "$O6" "$d" 15 60; done
fi

log "provider: FitCoach Szemelyi Edzes"
T3=$(ensure_approved_provider "trainer@example.com" "Szabó Bence" "FitCoach Személyi Edzés" "Egyénre szabott edzésprogramok, minden szinten." "$ADMIN_TOKEN")
if [ -n "$T3" ]; then
  O7=$(ensure_service_offering "$T3" "Személyi edzés" "60 perces 1-on-1 edzés." 60 9000)
  O8=$(ensure_service_offering "$T3" "Táplálkozási tanácsadás" "Étrend áttekintés és tervezés." 45 7000)
  for d in 1 2 3 4 5 8; do add_time_slot "$T3" "$O7" "$d" 17 60; done
  for d in 3 6; do add_time_slot "$T3" "$O8" "$d" 18 45; done
fi

log "provider: Nyugalom Sziget Masszazs"
T4=$(ensure_approved_provider "massage@example.com" "Tóth Réka" "Nyugalom Sziget Masszázs" "Relaxációs és sportmasszázs nyugodt környezetben." "$ADMIN_TOKEN")
if [ -n "$T4" ]; then
  O9=$(ensure_service_offering "$T4" "Relaxációs masszázs" "Teljes testes, feszültségoldó masszázs." 60 11000)
  O10=$(ensure_service_offering "$T4" "Sportmasszázs" "Célzott, mélyszöveti masszázs sportolóknak." 45 10000)
  O11=$(ensure_service_offering "$T4" "Talpmasszázs" "Reflexzóna-masszázs." 30 6500)
  for d in 1 3 5 7 9; do add_time_slot "$T4" "$O9" "$d" 14 60; done
  for d in 2 6; do add_time_slot "$T4" "$O10" "$d" 16 45; done
  for d in 4; do add_time_slot "$T4" "$O11" "$d" 12 30; done
fi

log "provider: AutoPro Szerviz"
T5=$(ensure_approved_provider "autoshop@example.com" "Horváth Gábor" "AutoPro Szerviz" "Megbízható autószerviz, garanciával." "$ADMIN_TOKEN")
if [ -n "$T5" ]; then
  O12=$(ensure_service_offering "$T5" "Olajcsere" "Motorolaj és szűrő csere." 30 15000)
  O13=$(ensure_service_offering "$T5" "Műszaki vizsga előkészítés" "Átvizsgálás, hibajavítás vizsga előtt." 90 20000)
  for d in 2 5 8; do add_time_slot "$T5" "$O12" "$d" 9 30; done
  for d in 3 10; do add_time_slot "$T5" "$O13" "$d" 10 90; done
fi

# ---------------------------------------------------------------------------
# Guests, some of whom book (and one who cancels) a few of the slots above
# ---------------------------------------------------------------------------

log "guest: Kiss Anna"
G1=$(register_or_login "seed.guest1@example.com" "Kiss Anna" | sed -n '1p')
log "guest: Varga Marton"
G2=$(register_or_login "seed.guest2@example.com" "Varga Márton" | sed -n '1p')
log "guest: Nemeth Zsofia"
G3=$(register_or_login "seed.guest3@example.com" "Németh Zsófia" | sed -n '1p')

book_first_available() {
  local guestToken="$1" offeringId="$2"
  local slotId
  slotId=$(curl -s "$BASE_URL/service-offerings/$offeringId/time-slots" -H "Authorization: Bearer $guestToken" \
    | python3 -c "import json,sys; d=json.load(sys.stdin); a=[s for s in d if s['status']=='AVAILABLE']; print(a[0]['id'] if a else '')")
  if [ -n "$slotId" ]; then
    curl -s -X POST "$BASE_URL/time-slots/$slotId/book" -H "Authorization: Bearer $guestToken" | json_get "['id']" 2>/dev/null || true
  fi
}

if [ -n "${O1:-}" ]; then log "  guest1 books a haircut"; book_first_available "$G1" "$O1" > /dev/null; fi
if [ -n "${O4:-}" ]; then log "  guest2 books a dental checkup"; book_first_available "$G2" "$O4" > /dev/null; fi
if [ -n "${O7:-}" ]; then log "  guest2 books a training session"; book_first_available "$G2" "$O7" > /dev/null; fi
if [ -n "${O9:-}" ]; then
  log "  guest3 books then cancels a massage (to show a cancelled booking)"
  BOOKING_ID=$(book_first_available "$G3" "$O9")
  if [ -n "$BOOKING_ID" ]; then
    curl -s -X DELETE "$BASE_URL/bookings/$BOOKING_ID" -H "Authorization: Bearer $G3" > /dev/null
  fi
fi

log "done. Seed accounts (password: $SEED_PASSWORD for all):"
log "  admin:    $ADMIN_EMAIL / $ADMIN_PASSWORD"
log "  provider: hairdresser@example.com, dentist@example.com, trainer@example.com, massage@example.com, autoshop@example.com"
log "  guest:    seed.guest1@example.com, seed.guest2@example.com, seed.guest3@example.com"
