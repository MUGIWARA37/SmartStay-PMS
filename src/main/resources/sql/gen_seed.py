#!/usr/bin/env python3
"""
SmartStay PMS — Dynamic Seed Generator
Generates a rolling dataset for:
- the previous 3 full years, and
- the entire current year.
Auto-generates seed.sql in the same directory.
Run: python3 gen_seed.py
"""

from datetime import date, datetime, timedelta
import random, math, sys

random.seed(42)
out = []

def emit(s=""): out.append(s)

# ────────────────────────────── date helpers ─────────────────────────────────
TODAY        = date.today()
CURRENT_YEAR = TODAY.year
START_DATE   = date(CURRENT_YEAR - 3, 1, 1)
END_DATE     = date(CURRENT_YEAR, 12, 31)

def fdate(d):  return d.strftime("'%Y-%m-%d'")
def fdatetime(dt): return f"'{dt.strftime('%Y-%m-%d %H:%M:%S')}'"

def rand_time(d, h0=7, h1=22):
    return datetime(d.year, d.month, d.day, random.randint(h0, h1), random.randint(0,59))

def add_days(d, n): return d + timedelta(days=n)

def last_day_of(y, m):
    if m == 12: return date(y, 12, 31)
    return date(y, m+1, 1) - timedelta(days=1)

def month_iter(start, end):
    y, m = start.year, start.month
    while (y, m) <= (end.year, end.month):
        yield (y, m)
        m += 1
        if m > 12: m, y = 1, y+1

def season_weight(d):
    """Probability multiplier for reservations starting on date d."""
    mo = d.month
    if mo in (7, 8):           return 1.45   # peak summer
    if mo in (12, 1):          return 1.30   # peak winter
    if mo in (6, 9):           return 1.15
    if mo in (10, 11):         return 0.95
    if mo in (2, 3):           return 0.80
    return 0.75                              # Apr–May shoulder

# ─────────────────────────── static reference data ───────────────────────────
ROOMS = [
    ('101','Single',   1), ('102','Single',  1), ('103','Single',  1), ('104','Single',  1),
    ('201','Double',   2), ('202','Double',  2), ('203','Twin',    2), ('204','Twin',    2),
    ('205','Double',   2), ('301','Deluxe',  3), ('302','Deluxe',  3), ('303','Deluxe',  3),
    ('304','Deluxe',   3), ('401','Suite',   4), ('402','Suite',   4), ('403','Suite',   4),
    ('501','Presidential', 5), ('502','Presidential', 5),
]
ROOM_PRICE = {
    'Single':350,'Double':550,'Twin':520,'Deluxe':750,'Suite':950,'Presidential':2200
}
RTYPE = {r[0]: r[1] for r in ROOMS}  # room_number → type_name

# service index → (sql-selector-by-code, unit_price)
SVCS = {
    1:('SVC-XFER',250.00,'Airport Transfer'),  2:('SVC-TOUR',400.00,'City Tour'),
    3:('SVC-BKFT', 85.00,'Breakfast Buffet'), 4:('SVC-ROOM', 50.00,'Room Service'),
    5:('SVC-DINE',650.00,'Romantic Dinner'),   6:('SVC-LAUN', 80.00,'Laundry Standard'),
    7:('SVC-EXPR',130.00,'Laundry Express'),   8:('SVC-SPA1',350.00,'Spa 60min'),
    9:('SVC-SPA2',480.00,'Spa 90min'),        10:('SVC-FIT', 200.00,'Personal Trainer'),
   11:('SVC-COT',  60.00,'Baby Cot'),         12:('SVC-BED', 120.00,'Extra Bed'),
   13:('SVC-LATE',150.00,'Late Checkout'),    14:('SVC-EARL',150.00,'Early Checkin'),
   15:('SVC-CNF4',500.00,'Conference Room 4h'),16:('SVC-CNF8',900.00,'Conference Room 8h'),
}
SVC_IDS = list(SVCS.keys())

# Room-type → most-likely services
ROOM_SVC_AFFINITY = {
    'Single':       [3, 6, 13],
    'Double':       [3, 6, 8, 13, 14],
    'Twin':         [3, 6, 13],
    'Deluxe':       [3, 5, 8, 9, 13, 14, 15],
    'Suite':        [3, 5, 8, 9, 10, 13, 14, 15, 16],
    'Presidential': [3, 5, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1, 2],
}

STAFF_CODES  = ['EMP-REC-001','EMP-REC-002','EMP-CLN-001','EMP-CLN-002','EMP-MNT-001','EMP-MNT-002']
STAFF_BASE   = {
    'EMP-REC-001':5200,'EMP-REC-002':4800,
    'EMP-CLN-001':3900,'EMP-CLN-002':3700,
    'EMP-MNT-001':4600,'EMP-MNT-002':4400,
}
STAFF_DEPT = {
    'EMP-REC-001':'reception','EMP-REC-002':'reception',
    'EMP-CLN-001':'cleaning', 'EMP-CLN-002':'cleaning',
    'EMP-MNT-001':'maintenance','EMP-MNT-002':'maintenance',
}

MAINT_POOL = [
    ('AC not cooling','AC unit not reaching set temperature — refrigerant may be low.','HIGH'),
    ('Leaking tap','Bathroom cold tap dripping — washer replacement needed.','MEDIUM'),
    ('Broken door lock','Lock mechanism jammed; guest cannot secure room.','HIGH'),
    ('Shower drain blocked','Slow drainage causing water to pool.','MEDIUM'),
    ('Flickering ceiling light','Ceiling light flickering — probable loose fitting.','LOW'),
    ('Cracked bathroom mirror','Mirror cracked at corner — safety hazard.','LOW'),
    ('TV unresponsive','Smart TV will not power on with remote or manual button.','MEDIUM'),
    ('AC compressor fault','Loud grinding from outdoor unit — compressor failure.','URGENT'),
    ('Large carpet stain','Significant stain requires specialist extraction.','LOW'),
    ('Toilet not flushing','Flush handle disconnected from siphon.','HIGH'),
    ('Window latch broken','Window will not latch — security and draught issue.','MEDIUM'),
    ('Safe keypad unresponsive','In-room safe keypad dead — batteries and board checked.','MEDIUM'),
    ('Power socket sparking','Wall socket sparking on plug insertion — urgent safety.','URGENT'),
    ('Elevator panel stuck','Floor button jammed in corridor panel.','HIGH'),
    ('Bathtub enamel chip','Chip on enamel surface — slip hazard.','LOW'),
    ('Loose handrail','Corridor handrail screws loose.','MEDIUM'),
    ('Extractor fan noisy','Bathroom extractor rattling loudly.','LOW'),
    ('Wardrobe door off hinge','Wardrobe door detached — hinge screw stripped.','MEDIUM'),
    ('Mattress spring broken','Guest reports spring poking through mattress.','HIGH'),
    ('Mini-bar not cooling','Mini-bar compressor not engaging — food spoilage risk.','HIGH'),
]

CLEANING_NOTES = [
    'Post checkout deep clean','Pre-arrival premium turndown','Daily service',
    'Guest requested extra towels and toiletries','VIP arrival — silver-service setup',
    'Linen change only','Full deep clean and sanitisation',
    'Baby cot setup required','Late checkout — clean after 14:00',
    'Extra blankets requested','Hypoallergenic bedding required',
    'Complimentary fruit basket to be placed','Champagne on ice for anniversary guests',
    'Guest has pet — extra vacuum needed','Sanitise all high-touch surfaces',
]

SPECIAL_REQS = [
    'High floor preferred','Sea view if possible','Quiet room away from elevator',
    'Extra pillows and blankets','Late checkout needed','Early check-in from 09:00',
    'Vegetarian meals throughout stay','Allergy — no feather pillows',
    'Baby cot required for infant','Business amenities and printer access',
    'Honeymoon setup — rose petals and champagne','VIP treatment — loyal return guest',
    'Connecting rooms for family group','Butler on call 24/7',
    'No smoking room essential','Airport transfer required on arrival',
    'Dietary restriction — halal meals only','Celebrating birthday — decoration welcome',
    'Medical equipment — power supply needed near bed',
    None, None, None, None,  # ~20% no special request
]

NATIONALITIES = [
    ('French','FR'),('British','GB'),('Spanish','ES'),('Korean','KR'),('American','US'),
    ('Moroccan','MA'),('Nigerian','NG'),('Japanese','JP'),('German','DE'),('Ghanaian','GH'),
    ('Italian','IT'),('Dutch','NL'),('Canadian','CA'),('Brazilian','BR'),('Emirati','AE'),
    ('Saudi','SA'),('Turkish','TR'),('Indian','IN'),('Chinese','CN'),('Russian','RU'),
    ('Australian','AU'),('Mexican','MX'),('South African','ZA'),('Argentine','AR'),('Swedish','SE'),
]
FIRST = [
    'Alice','Bob','Clara','David','Emma','Farid','Grace','Hiro','Ines','James',
    'Karim','Lena','Mohamed','Nadia','Omar','Pierre','Quinn','Rania','Sofia','Tariq',
    'Ursula','Valeria','Wei','Xander','Yuki','Zara','Ahmed','Beatrice','Carlos','Diana',
    'Ethan','Fatima','George','Hana','Ivan','Julia','Kenji','Luna','Matteo','Nour',
    'Oscar','Priya','Rafael','Samira','Tomas','Uma','Victor','Wendy','Xander2','Yasmine',
    'Zaid','Amira','Bruno','Chiara','Darius','Elena','Finn','Giulia','Hassan','Irina',
    'Jasper','Kira','Leon','Maria','Nico','Olivia','Pablo','Rosa','Sven','Talia',
    'Umar','Vera','William','Xiomara','Yann','Zeinab','Adil','Brigitte','Cosimo','Dalila',
    'Emeka','Fumiko','Gerardo','Hilary','Idris','Jana','Khalid','Laila','Marcos','Nkechi',
]
LAST = [
    'Martin','Jones','Santos','Kim','Wilson','Benali','Okafor','Tanaka','Dupont','Osei',
    'Alami','Muller','El Fassi','Chraibi','Idrissi','Moreau','Leroy','Garcia','Schmidt','Rossi',
    'Brown','Taylor','Anderson','Thomas','Jackson','White','Harris','Clark','Lewis','Robinson',
    'Walker','Hall','Allen','Young','King','Wright','Scott','Green','Baker','Adams',
    'Nelson','Carter','Mitchell','Perez','Roberts','Turner','Phillips','Campbell','Parker','Evans',
    'Edwards','Collins','Stewart','Morris','Rogers','Reed','Cook','Morgan','Bell','Murphy',
    'Yamamoto','Nakamura','Suzuki','Ito','Watanabe','Kobayashi','Saito','Kato','Abe','Hayashi',
    'Mensah','Diallo','Traoré','Konaté','Kofi','Asante','Abebe','Gebre','Tadesse','Bekele',
    'Rashid','Haddad','Mansour','Nassar','Qureshi','Sheikh','Anand','Sharma','Patel','Singh',
]

def unique_name(used):
    for _ in range(1000):
        f = random.choice(FIRST)
        l = random.choice(LAST)
        key = (f, l)
        if key not in used:
            used.add(key)
            return f, l
    return 'Guest', f'Unknown{len(used)}'

# ─────────────────────── build reservation calendar ──────────────────────────
# Strategy: simulate each room independently across the 3-year span.
# Each room gets a sequence of non-overlapping stays with realistic gaps.

reservations  = []   # dict per reservation
guests_needed = []   # (first, last, email, phone, nationality, passport, pref, created_at)
res_counter   = [0]

def next_res_code():
    res_counter[0] += 1
    return f'RES-HIST-{res_counter[0]:04d}'

used_names   = set()
used_emails  = set()

def gen_guest(created_at_date):
    fn, ln = unique_name(used_names)
    nat, cc = random.choice(NATIONALITIES)
    num = random.randint(100000, 999999)
    fn_token = fn.lower().replace(" ", "")
    ln_token = ln.lower().replace(" ", "").replace("'", "")
    base_email = f"{fn_token}.{ln_token}@mail.com"
    email = base_email
    attempt = 1
    while email in used_emails:
        email = f"{fn_token}_{attempt}@mail.com"
        attempt += 1
    used_emails.add(email)
    phone = f"+2126{random.randint(10,99)}{random.randint(100000,999999)}"
    passport = f"{cc}{num}"
    pref = random.choice(SPECIAL_REQS)
    created_at = datetime(created_at_date.year, created_at_date.month,
                          created_at_date.day, random.randint(8,20), random.randint(0,59))
    return {
        'first': fn, 'last': ln, 'email': email, 'phone': phone,
        'nationality': nat, 'passport': passport, 'pref': pref,
        'created_at': created_at,
        'idx': len(guests_needed),  # 0-based index
    }

# Guest pool already seeded (the 15 seed guests keep their user accounts)
# We generate ADDITIONAL anonymous guests for historical bookings.
# The seeded client users (client1..client10) map to the first 10 seed guests.
# Historical bookings use booked_by_user_id from client users randomly.

CLIENT_USER_IDS = list(range(8, 18))   # users table: client1=8..client10=17 (1-based rows)
# We'll use SELECT subqueries so actual IDs don't matter.
CLIENT_USERNAMES = [f'client{i}' for i in range(1,11)]
STAFF_USERNAMES  = ['reception1','reception2']

def booked_by_expr(booking_date):
    # 80% clients book themselves, 20% reception walks in
    if random.random() < 0.80:
        u = random.choice(CLIENT_USERNAMES)
    else:
        u = random.choice(STAFF_USERNAMES)
    return f"(SELECT id FROM users WHERE username='{u}')"

# ─── simulate room calendars ─────────────────────────────────────────────────
# Each room: walk from START_DATE to END_DATE
# Probability of a stay starting on any given available day = base_prob * season_weight

BASE_PROB = {
    'Single':0.40,'Double':0.45,'Twin':0.38,'Deluxe':0.35,'Suite':0.25,'Presidential':0.12
}

guest_pool = []  # will be indexed; each entry is a guest dict

room_reservations = []  # final flat list

for rno, rtype, rfloor in ROOMS:
    prob = BASE_PROB[rtype]
    cursor = START_DATE
    while cursor <= END_DATE:
        sw = season_weight(cursor)
        if random.random() < prob * sw:
            # Generate a stay
            nights = random.choices(
                [1,2,3,4,5,6,7,10,14,21],
                weights=[10,18,20,17,12,8,6,4,3,2]
            )[0]
            ci = cursor
            co = add_days(ci, nights)
            if co > add_days(END_DATE, 1):
                co = add_days(END_DATE, 1)
                nights = (co - ci).days
                if nights == 0:
                    cursor = add_days(cursor, 1)
                    continue

            # Determine status
            if co < TODAY:
                # Past: mostly CHECKED_OUT, rare CANCELLED
                if random.random() < 0.08:
                    status = 'CANCELLED'
                else:
                    status = 'CHECKED_OUT'
            elif ci <= TODAY <= co:
                status = 'CHECKED_IN'
            else:
                # Future reservations within the rest of the current year
                status = 'CONFIRMED'

            # Pick or create a guest (reuse existing 15 seed guests ~20% of time)
            if random.random() < 0.20 and len(guest_pool) == 0:
                # No pool yet — create new
                pass
            
            # Always create a fresh guest for historical data variety
            created_at = rand_time(add_days(ci, -random.randint(1,30)), 8, 21)
            if created_at.date() < START_DATE:
                created_at = rand_time(START_DATE, 8, 21)
            g = gen_guest(created_at.date())
            guest_pool.append(g)
            g_idx = g['idx']

            adults   = random.choices([1,2,3,4],[40,40,12,8])[0]
            children = random.choices([0,1,2],[70,20,10])[0]

            room_reservations.append({
                'guest_idx':  g_idx,
                'room_no':    rno,
                'rtype':      rtype,
                'code':       next_res_code(),
                'ci':         ci,
                'co':         co,
                'nights':     nights,
                'adults':     adults,
                'children':   children,
                'status':     status,
                'special':    random.choice(SPECIAL_REQS),
                'booked_by':  booked_by_expr(ci),
                'created_at': created_at,
            })

            # Gap between stays (cleaning + buffer)
            gap = random.randint(0, 2)
            cursor = add_days(co, gap)
        else:
            cursor = add_days(cursor, 1)

print(f"Generated {len(room_reservations)} reservations, {len(guest_pool)} guests",
      file=sys.stderr)

# ─── Now write the SQL ────────────────────────────────────────────────────────
emit("-- ═══════════════════════════════════════════════════════════════════════")
emit(f"--  SmartStay PMS — Rolling Seed ({START_DATE.isoformat()} → {END_DATE.isoformat()})")
emit("--  Generated with gen_seed.py  |  DO NOT EDIT MANUALLY")
emit("--")
emit("--  Passwords:")
emit("--    admin123  → admin")
emit("--    staff123  → all staff")
emit("--    client123 → all clients")
emit("--")
emit("--  Run:")
emit("--    docker exec -i smartstay-db mysql -u root -pDB_Password123! smartstay < seed.sql")
emit("-- ═══════════════════════════════════════════════════════════════════════")
emit()
emit("USE smartstay;")
emit("SET FOREIGN_KEY_CHECKS = 0;")
emit()

# TRUNCATE
for tbl in [
    'reservation_services','invoice_lines','payments','invoices',
    'cleaning_requests','maintenance_requests','staff_shift_assignments',
    'staff_attendance','shifts','payroll','reservations',
    'user_security_answers','password_reset_audit','password_reset_challenges',
    'security_questions','room_images','services','staff_profiles',
    'guests','rooms','room_types','users',
]:
    emit(f"TRUNCATE TABLE {tbl};")

emit()
emit("SET FOREIGN_KEY_CHECKS = 1;")
emit()

# ─────────────── 1. USERS ────────────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 1. USERS")
emit("-- ══════════════════════════════════════════════════════════════")
emit("""INSERT INTO users (username, email, password_hash, role, is_active, last_login_at, created_at) VALUES
('admin',      'admin@smartstay.local',   '$2a$10$3JgR5c6DWukENJ3UeyDmZeAvviMQnPyVVkCEmvhVjbl0zMOvDz7kO', 'ADMIN',  1, DATE_SUB(NOW(), INTERVAL 1 HOUR),    DATE_SUB(NOW(), INTERVAL 60 DAY)),
('reception1', 'rec1@smartstay.local',    '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, DATE_SUB(NOW(), INTERVAL 2 HOUR),    DATE_SUB(NOW(), INTERVAL 55 DAY)),
('reception2', 'rec2@smartstay.local',    '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, DATE_SUB(NOW(), INTERVAL 1 DAY),     DATE_SUB(NOW(), INTERVAL 50 DAY)),
('cleaning1',  'clean1@smartstay.local',  '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, DATE_SUB(NOW(), INTERVAL 3 HOUR),    DATE_SUB(NOW(), INTERVAL 48 DAY)),
('cleaning2',  'clean2@smartstay.local',  '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, NULL,                                DATE_SUB(NOW(), INTERVAL 40 DAY)),
('maint1',     'maint1@smartstay.local',  '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, DATE_SUB(NOW(), INTERVAL 5 HOUR),    DATE_SUB(NOW(), INTERVAL 45 DAY)),
('maint2',     'maint2@smartstay.local',  '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  0, NULL,                                DATE_SUB(NOW(), INTERVAL 30 DAY)),
('client1',    'alice.martin@mail.com',   '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR),    DATE_SUB(NOW(), INTERVAL 30 DAY)),
('client2',    'bob.jones@mail.com',      '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 1 DAY),     DATE_SUB(NOW(), INTERVAL 28 DAY)),
('client3',    'clara.santos@mail.com',   '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, NULL,                                DATE_SUB(NOW(), INTERVAL 25 DAY)),
('client4',    'david.kim@mail.com',      '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 4 HOUR),    DATE_SUB(NOW(), INTERVAL 20 DAY)),
('client5',    'emma.wilson@mail.com',    '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, NULL,                                DATE_SUB(NOW(), INTERVAL 18 DAY)),
('client6',    'farid.benali@mail.com',   '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 6 HOUR),    DATE_SUB(NOW(), INTERVAL 15 DAY)),
('client7',    'grace.okafor@mail.com',   '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, NULL,                                DATE_SUB(NOW(), INTERVAL 12 DAY)),
('client8',    'hiro.tanaka@mail.com',    '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 10 DAY)),
('client9',    'ines.dupont@mail.com',    '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 0, NULL,                                DATE_SUB(NOW(), INTERVAL  8 DAY)),
('client10',   'james.osei@mail.com',     '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 3 DAY),     DATE_SUB(NOW(), INTERVAL  5 DAY));
""")

# ─────────────── 2. SECURITY QUESTIONS ──────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 2. SECURITY QUESTIONS")
emit("-- ══════════════════════════════════════════════════════════════")
emit("""INSERT INTO security_questions (question_text, is_active) VALUES
('What was your first pet\\'s name?',           1),
('What is your mother\\'s maiden name?',        1),
('What city were you born in?',                1),
('What was the name of your primary school?',  1),
('What is your oldest sibling\\'s middle name?', 1);

INSERT INTO user_security_answers (user_id, question_id, answer_hash, created_at) VALUES
((SELECT id FROM users WHERE username='admin'),      1, SHA2('dragon',     256), NOW()),
((SELECT id FROM users WHERE username='reception1'), 2, SHA2('smith',      256), NOW()),
((SELECT id FROM users WHERE username='reception2'), 3, SHA2('casablanca', 256), NOW()),
((SELECT id FROM users WHERE username='cleaning1'),  1, SHA2('milo',       256), NOW()),
((SELECT id FROM users WHERE username='maint1'),     4, SHA2('ibnbatuta',  256), NOW()),
((SELECT id FROM users WHERE username='client1'),    3, SHA2('paris',      256), NOW()),
((SELECT id FROM users WHERE username='client2'),    2, SHA2('jones',      256), NOW()),
((SELECT id FROM users WHERE username='client4'),    5, SHA2('thomas',     256), NOW()),
((SELECT id FROM users WHERE username='client8'),    1, SHA2('hachi',      256), NOW());
""")

# ─────────────── 3. ROOM TYPES ───────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 3. ROOM TYPES")
emit("-- ══════════════════════════════════════════════════════════════")
emit("""INSERT INTO room_types (name, description, price_per_night, max_occupancy, amenities) VALUES
('Single',       'Cozy room for solo travelers',                  350.00, 1, 'WiFi,Desk,TV,Safe'),
('Double',       'Comfortable room for couples',                  550.00, 2, 'WiFi,TV,Mini Bar,Safe'),
('Twin',         'Two separate beds, ideal for friends',          520.00, 2, 'WiFi,TV,Mini Bar'),
('Deluxe',       'Spacious deluxe room with premium fittings',    750.00, 3, 'WiFi,TV,Mini Bar,Safe,Bathtub'),
('Suite',        'Premium suite with lounge and kitchenette',     950.00, 4, 'WiFi,TV,Mini Bar,Jacuzzi,Bathtub,Lounge'),
('Presidential', 'Exclusive top-floor presidential suite',       2200.00, 6, 'WiFi,TV,Mini Bar,Jacuzzi,Bathtub,Lounge,Butler,Kitchen');
""")

# ─────────────── 4. ROOMS ────────────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 4. ROOMS")
emit("-- ══════════════════════════════════════════════════════════════")
emit("""INSERT INTO rooms (room_number, room_type_id, floor, status, notes) VALUES
('101', (SELECT id FROM room_types WHERE name='Single'),       1, 'AVAILABLE',   'Near elevator'),
('102', (SELECT id FROM room_types WHERE name='Single'),       1, 'OCCUPIED',    NULL),
('103', (SELECT id FROM room_types WHERE name='Single'),       1, 'AVAILABLE',   'Quiet side'),
('104', (SELECT id FROM room_types WHERE name='Single'),       1, 'CLEANING',    NULL),
('201', (SELECT id FROM room_types WHERE name='Double'),       2, 'AVAILABLE',   NULL),
('202', (SELECT id FROM room_types WHERE name='Double'),       2, 'OCCUPIED',    NULL),
('203', (SELECT id FROM room_types WHERE name='Twin'),         2, 'AVAILABLE',   'Corner room, extra light'),
('204', (SELECT id FROM room_types WHERE name='Twin'),         2, 'MAINTENANCE', 'Shower drain blocked'),
('205', (SELECT id FROM room_types WHERE name='Double'),       2, 'AVAILABLE',   NULL),
('301', (SELECT id FROM room_types WHERE name='Deluxe'),       3, 'AVAILABLE',   'City view'),
('302', (SELECT id FROM room_types WHERE name='Deluxe'),       3, 'OCCUPIED',    NULL),
('303', (SELECT id FROM room_types WHERE name='Deluxe'),       3, 'AVAILABLE',   'Mountain view'),
('304', (SELECT id FROM room_types WHERE name='Deluxe'),       3, 'CLEANING',    NULL),
('401', (SELECT id FROM room_types WHERE name='Suite'),        4, 'AVAILABLE',   'Sea view, jacuzzi'),
('402', (SELECT id FROM room_types WHERE name='Suite'),        4, 'OCCUPIED',    'VIP guest'),
('403', (SELECT id FROM room_types WHERE name='Suite'),        4, 'MAINTENANCE', 'AC compressor fault'),
('501', (SELECT id FROM room_types WHERE name='Presidential'), 5, 'AVAILABLE',   'Full panoramic view, butler service'),
('502', (SELECT id FROM room_types WHERE name='Presidential'), 5, 'OCCUPIED',    'Long-stay VIP');
""")

# ─────────────── 5. ROOM IMAGES ──────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 5. ROOM IMAGES")
emit("-- ══════════════════════════════════════════════════════════════")

# Map room type to image filename (lowercase)
TYPE_TO_IMAGE = {
    'Single': 'single',
    'Double': 'double',
    'Twin': 'twin',
    'Deluxe': 'deluxe',
    'Suite': 'suite',
    'Presidential': 'presidential'
}

emit("INSERT INTO room_images (room_id, image_path, is_primary, sort_order) VALUES")
lines = []
for i, (room_num, room_type, capacity) in enumerate(ROOMS):
    image_filename = TYPE_TO_IMAGE.get(room_type, 'default')
    image_path = f"/images/rooms/{image_filename}.jpg"
    line = f"((SELECT id FROM rooms WHERE room_number='{room_num}'), '{image_path}', 1, 1)"
    lines.append(line)

emit(",\n".join(lines) + ";")


# ─────────────── 6. SERVICES ─────────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 6. SERVICES")
emit("-- ══════════════════════════════════════════════════════════════")
emit("""INSERT INTO services (code, name, description, unit_price, is_active) VALUES
('SVC-XFER',  'Airport Transfer',      'Round-trip airport pickup and drop-off',    250.00, 1),
('SVC-TOUR',  'City Tour',             'Full-day guided city tour (8h)',             400.00, 1),
('SVC-BKFT',  'Breakfast Buffet',      'Daily breakfast buffet per person',           85.00, 1),
('SVC-ROOM',  'Room Service',          'In-room dining (menu + 15% fee)',             50.00, 1),
('SVC-DINE',  'Romantic Dinner',       'Private candlelit dinner for two',           650.00, 1),
('SVC-LAUN',  'Laundry Standard',      'Same-day laundry per bag',                    80.00, 1),
('SVC-EXPR',  'Laundry Express',       '4-hour express laundry per bag',             130.00, 1),
('SVC-SPA1',  'Spa 60min',             'Full-body relaxation massage',               350.00, 1),
('SVC-SPA2',  'Spa 90min',             'Deep tissue massage -- 90 minutes',          480.00, 1),
('SVC-FIT',   'Personal Trainer',      'One-hour private fitness session',           200.00, 1),
('SVC-COT',   'Baby Cot',              'Foldable cot with bedding per night',         60.00, 1),
('SVC-BED',   'Extra Bed',             'Additional bed setup per night',             120.00, 1),
('SVC-LATE',  'Late Checkout',         'Checkout extended to 14:00',                 150.00, 1),
('SVC-EARL',  'Early Checkin',         'Checkin from 08:00 instead of 14:00',        150.00, 1),
('SVC-CNF4',  'Conference Room (4h)',  'Private meeting room -- half day',           500.00, 1),
('SVC-CNF8',  'Conference Room (8h)',  'Private meeting room -- full day',           900.00, 1);
""")

# ─────────────── 7. GUESTS ───────────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 7. GUESTS  (15 seed guests + all historical guests)")
emit("-- ══════════════════════════════════════════════════════════════")

# Seed guests (must match client1..client10 email + extras)
seed_guests_sql = """INSERT INTO guests (first_name, last_name, email, phone, nationality, id_passport_number, preferences, created_at) VALUES
('Alice',   'Martin',   'alice.martin@mail.com',  '+212611000001', 'French',    'FR123456', 'High floor, twin beds',             DATE_SUB(NOW(), INTERVAL 30 DAY)),
('Bob',     'Jones',    'bob.jones@mail.com',      '+212611000002', 'British',   'GB234567', 'Quiet room, no smoking',            DATE_SUB(NOW(), INTERVAL 28 DAY)),
('Clara',   'Santos',   'clara.santos@mail.com',   '+212611000003', 'Spanish',   'ES345678', 'Sea view if possible',              DATE_SUB(NOW(), INTERVAL 25 DAY)),
('David',   'Kim',      'david.kim@mail.com',       '+212611000004', 'Korean',    'KR456789', 'Extra pillows, late checkout',      DATE_SUB(NOW(), INTERVAL 20 DAY)),
('Emma',    'Wilson',   'emma.wilson@mail.com',    '+212611000005', 'American',  'US567890', 'Vegetarian meals',                  DATE_SUB(NOW(), INTERVAL 18 DAY)),
('Farid',   'Benali',   'farid.benali@mail.com',   '+212611000006', 'Moroccan',  'MA678901', 'Ground floor preferred',            DATE_SUB(NOW(), INTERVAL 15 DAY)),
('Grace',   'Okafor',   'grace.okafor@mail.com',   '+212611000007', 'Nigerian',  'NG789012', 'Allergy: feather pillows',          DATE_SUB(NOW(), INTERVAL 12 DAY)),
('Hiro',    'Tanaka',   'hiro.tanaka@mail.com',    '+212611000008', 'Japanese',  'JP890123', 'Western breakfast, early checkin',  DATE_SUB(NOW(), INTERVAL 10 DAY)),
('Ines',    'Dupont',   'ines.dupont@mail.com',    '+212611000009', 'French',    'FR901234', 'Connecting rooms for family',       DATE_SUB(NOW(), INTERVAL  8 DAY)),
('James',   'Osei',     'james.osei@mail.com',     '+212611000010', 'Ghanaian',  'GH012345', 'Business amenities, printer',       DATE_SUB(NOW(), INTERVAL  5 DAY)),
('Karim',   'Alami',    'karim.alami@gmail.com',   '+212655000011', 'Moroccan',  'MA111111', NULL,                                DATE_SUB(NOW(), INTERVAL  3 DAY)),
('Lena',    'Muller',   'lena.muller@gmail.com',   '+49300000012',  'German',    'DE222222', 'Quiet room, city view',             DATE_SUB(NOW(), INTERVAL  2 DAY)),
('Mohamed', 'El Fassi', 'melfassi@gmail.com',      '+212666000013', 'Moroccan',  'MA333333', NULL,                                DATE_SUB(NOW(), INTERVAL  1 DAY)),
('Nadia',   'Chraibi',  'nadia.chraibi@gmail.com', '+212677000014', 'Moroccan',  'MA444444', 'High floor, jacuzzi',               NOW()),
('Omar',    'Idrissi',  'omar.idrissi@gmail.com',  '+212688000015', 'Moroccan',  'MA555555', NULL,                                NOW());"""
emit(seed_guests_sql)
emit()

# Historical guests in batches of 100
BATCH = 100
hist_rows = []
for g in guest_pool:
    fn   = g['first'].replace("'", "\\'")
    ln   = g['last'].replace("'", "\\'")
    em   = g['email']
    ph   = g['phone']
    nat  = g['nationality'].replace("'", "\\'")
    pp   = g['passport']
    pr   = g['pref']
    ca   = fdatetime(g['created_at'])
    pref_sql = f"'{pr.replace(chr(39), chr(39)+chr(39))}'" if pr else 'NULL'
    hist_rows.append(
        f"('{fn}', '{ln}', '{em}', '{ph}', '{nat}', '{pp}', {pref_sql}, {ca})"
    )

emit(f"-- {len(hist_rows)} additional historical guests")
for i in range(0, len(hist_rows), BATCH):
    chunk = hist_rows[i:i+BATCH]
    emit("INSERT INTO guests (first_name, last_name, email, phone, nationality, id_passport_number, preferences, created_at) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# ─────────────── 8. STAFF PROFILES ───────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 8. STAFF PROFILES")
emit("-- ══════════════════════════════════════════════════════════════")
emit("""INSERT INTO staff_profiles (user_id, employee_code, position, department, hire_date, salary_base, emergency_contact, is_on_duty) VALUES
((SELECT id FROM users WHERE username='reception1'), 'EMP-REC-001', 'reception',   'Front Office', DATE_SUB(CURDATE(), INTERVAL 3 YEAR),  5200.00, '0600000001', 1),
((SELECT id FROM users WHERE username='reception2'), 'EMP-REC-002', 'reception',   'Front Office', DATE_SUB(CURDATE(), INTERVAL 1 YEAR),  4800.00, '0600000002', 1),
((SELECT id FROM users WHERE username='cleaning1'),  'EMP-CLN-001', 'cleaning',    'Housekeeping', DATE_SUB(CURDATE(), INTERVAL 2 YEAR),  3900.00, '0600000003', 1),
((SELECT id FROM users WHERE username='cleaning2'),  'EMP-CLN-002', 'cleaning',    'Housekeeping', DATE_SUB(CURDATE(), INTERVAL 6 MONTH), 3700.00, '0600000004', 0),
((SELECT id FROM users WHERE username='maint1'),     'EMP-MNT-001', 'maintenance', 'Engineering',  DATE_SUB(CURDATE(), INTERVAL 4 YEAR),  4600.00, '0600000005', 1),
((SELECT id FROM users WHERE username='maint2'),     'EMP-MNT-002', 'maintenance', 'Engineering',  DATE_SUB(CURDATE(), INTERVAL 8 MONTH), 4400.00, '0600000006', 0);
""")

# ─────────────── 9. SHIFTS ───────────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 9. SHIFTS")
emit("-- ══════════════════════════════════════════════════════════════")
emit("""INSERT INTO shifts (shift_name, start_time, end_time, is_night_shift) VALUES
('Morning Reception',    '06:00:00', '14:00:00', 0),
('Evening Reception',    '14:00:00', '22:00:00', 0),
('Night Reception',      '22:00:00', '06:00:00', 1),
('Morning Housekeeping', '07:00:00', '15:00:00', 0),
('Evening Housekeeping', '15:00:00', '23:00:00', 0),
('Morning Maintenance',  '08:00:00', '16:00:00', 0),
('OnCall Maintenance',   '16:00:00', '08:00:00', 1);
""")

# ─────────────── 10. SHIFT ASSIGNMENTS (3 years, every workday) ──────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 10. SHIFT ASSIGNMENTS (3 years of weekly schedules)")
emit("-- ══════════════════════════════════════════════════════════════")

# Map: emp_code → shift_name
EMP_SHIFT = {
    'EMP-REC-001': 'Morning Reception',
    'EMP-REC-002': 'Evening Reception',
    'EMP-CLN-001': 'Morning Housekeeping',
    'EMP-CLN-002': 'Evening Housekeeping',
    'EMP-MNT-001': 'Morning Maintenance',
    'EMP-MNT-002': 'OnCall Maintenance',
}

shift_rows = []
cur = START_DATE
while cur <= END_DATE:
    dow = cur.weekday()  # 0=Mon … 6=Sun
    for emp, shift in EMP_SHIFT.items():
        # Reception: 6 days/week (Mon-Sat); Cleaning/Maint: 5 days (Mon-Fri)
        dept = STAFF_DEPT[emp]
        if dept == 'reception' and dow < 6:
            shift_rows.append(
                f"((SELECT id FROM staff_profiles WHERE employee_code='{emp}'), "
                f"(SELECT id FROM shifts WHERE shift_name='{shift}'), "
                f"{fdate(cur)}, "
                f"(SELECT id FROM users WHERE username='admin'))"
            )
        elif dept in ('cleaning','maintenance') and dow < 5:
            shift_rows.append(
                f"((SELECT id FROM staff_profiles WHERE employee_code='{emp}'), "
                f"(SELECT id FROM shifts WHERE shift_name='{shift}'), "
                f"{fdate(cur)}, "
                f"(SELECT id FROM users WHERE username='admin'))"
            )
    cur = add_days(cur, 1)

emit(f"-- {len(shift_rows)} shift assignment rows")
for i in range(0, len(shift_rows), BATCH):
    chunk = shift_rows[i:i+BATCH]
    emit("INSERT INTO staff_shift_assignments (staff_profile_id, shift_id, assigned_date, assigned_by_user_id) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# ─────────────── 11. STAFF ATTENDANCE ────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 11. STAFF ATTENDANCE (3 years, realistic absences)")
emit("-- ══════════════════════════════════════════════════════════════")

att_rows = []
cur = START_DATE
while cur <= END_DATE:
    dow = cur.weekday()
    for emp in STAFF_CODES:
        dept = STAFF_DEPT[emp]
        works_today = (dept == 'reception' and dow < 6) or (dept != 'reception' and dow < 5)
        if not works_today:
            continue
        roll = random.random()
        if roll < 0.015:     att_status = 'ABSENT'; note = "'Sick leave'"
        elif roll < 0.035:   att_status = 'LATE';   note = "'Traffic delay'"
        elif roll < 0.050:   att_status = 'LEAVE';  note = "'Annual leave'"
        else:                att_status = 'PRESENT'; note = 'NULL'

        if att_status == 'ABSENT':
            ci_sql = 'NULL'; co_sql = 'NULL'
        else:
            h_in  = random.randint(6, 8) if att_status == 'PRESENT' else random.randint(8,10)
            ci_dt = datetime(cur.year, cur.month, cur.day, h_in, random.randint(0,59))
            co_dt = datetime(cur.year, cur.month, cur.day, h_in+7, random.randint(0,59))
            ci_sql = fdatetime(ci_dt)
            co_sql = fdatetime(co_dt)

        att_rows.append(
            f"((SELECT id FROM staff_profiles WHERE employee_code='{emp}'), "
            f"{fdate(cur)}, {ci_sql}, {co_sql}, '{att_status}', {note})"
        )
    cur = add_days(cur, 1)

emit(f"-- {len(att_rows)} attendance rows")
for i in range(0, len(att_rows), BATCH):
    chunk = att_rows[i:i+BATCH]
    emit("INSERT INTO staff_attendance (staff_profile_id, date, check_in, check_out, status, notes) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# ─────────────── 12. RESERVATIONS ────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 12. RESERVATIONS (historical + seed current stays)")
emit("-- ══════════════════════════════════════════════════════════════")

# guest_pool entries are 0-indexed; MySQL guests table rows are:
#  1..15 = seed guests, 16..N = historical guests
# So historical guest row = g_idx + 16  (1-based)

res_rows = []
for r in room_reservations:
    g = guest_pool[r['guest_idx']]
    em = g['email']
    sp = r['special']
    sp_sql = f"'{sp.replace(chr(39), chr(39)+chr(39))}'" if sp else 'NULL'
    res_rows.append(
        f"((SELECT id FROM guests WHERE email='{em}'), "
        f"(SELECT id FROM rooms WHERE room_number='{r['room_no']}'), "
        f"{r['booked_by']}, "
        f"'{r['code']}', "
        f"{fdate(r['ci'])}, {fdate(r['co'])}, "
        f"{r['adults']}, {r['children']}, "
        f"'{r['status']}', {sp_sql})"
    )

emit(f"-- {len(res_rows)} historical reservations")
for i in range(0, len(res_rows), 200):
    chunk = res_rows[i:i+200]
    emit("INSERT INTO reservations (guest_id, room_id, booked_by_user_id, reservation_code, check_in_date, check_out_date, adults_count, children_count, status, special_requests) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# Seed current/future reservations (original 16 from the original seed)
emit("-- Seed current/upcoming reservations (matching live room statuses)")
emit(f"""INSERT INTO reservations (guest_id, room_id, booked_by_user_id, reservation_code, check_in_date, check_out_date, adults_count, children_count, status, special_requests) VALUES
((SELECT id FROM guests WHERE email='hiro.tanaka@mail.com'),   (SELECT id FROM rooms WHERE room_number='102'), (SELECT id FROM users WHERE username='client8'),    'RES-{CURRENT_YEAR}-0006', DATE_SUB(CURDATE(),INTERVAL  2 DAY), DATE_ADD(CURDATE(),INTERVAL  3 DAY), 1, 0, 'CHECKED_IN',  'Early breakfast daily'),
((SELECT id FROM guests WHERE email='farid.benali@mail.com'),  (SELECT id FROM rooms WHERE room_number='202'), (SELECT id FROM users WHERE username='client6'),    'RES-{CURRENT_YEAR}-0007', DATE_SUB(CURDATE(),INTERVAL  1 DAY), DATE_ADD(CURDATE(),INTERVAL  4 DAY), 2, 1, 'CHECKED_IN',  NULL),
((SELECT id FROM guests WHERE email='lena.muller@gmail.com'),  (SELECT id FROM rooms WHERE room_number='302'), (SELECT id FROM users WHERE username='reception1'), 'RES-{CURRENT_YEAR}-0008', DATE_SUB(CURDATE(),INTERVAL  3 DAY), DATE_ADD(CURDATE(),INTERVAL  2 DAY), 2, 0, 'CHECKED_IN',  'City view essential'),
((SELECT id FROM guests WHERE email='james.osei@mail.com'),    (SELECT id FROM rooms WHERE room_number='402'), (SELECT id FROM users WHERE username='client10'),   'RES-{CURRENT_YEAR}-0009', CURDATE(),                          DATE_ADD(CURDATE(),INTERVAL  5 DAY), 1, 0, 'CHECKED_IN',  'VIP treatment'),
((SELECT id FROM guests WHERE email='nadia.chraibi@gmail.com'),(SELECT id FROM rooms WHERE room_number='502'), (SELECT id FROM users WHERE username='reception2'), 'RES-{CURRENT_YEAR}-0010', DATE_SUB(CURDATE(),INTERVAL  5 DAY), DATE_ADD(CURDATE(),INTERVAL 10 DAY), 2, 2, 'CHECKED_IN',  'Butler on call 24/7'),
((SELECT id FROM guests WHERE email='emma.wilson@mail.com'),   (SELECT id FROM rooms WHERE room_number='203'), (SELECT id FROM users WHERE username='client5'),    'RES-{CURRENT_YEAR}-0011', DATE_ADD(CURDATE(),INTERVAL  2 DAY), DATE_ADD(CURDATE(),INTERVAL  6 DAY), 2, 0, 'CONFIRMED',   'Vegetarian menu'),
((SELECT id FROM guests WHERE email='grace.okafor@mail.com'),  (SELECT id FROM rooms WHERE room_number='301'), (SELECT id FROM users WHERE username='client7'),    'RES-{CURRENT_YEAR}-0012', DATE_ADD(CURDATE(),INTERVAL  5 DAY), DATE_ADD(CURDATE(),INTERVAL  9 DAY), 1, 0, 'CONFIRMED',   'No feather pillows'),
((SELECT id FROM guests WHERE email='omar.idrissi@gmail.com'), (SELECT id FROM rooms WHERE room_number='104'), (SELECT id FROM users WHERE username='reception1'), 'RES-{CURRENT_YEAR}-0013', DATE_ADD(CURDATE(),INTERVAL  1 DAY), DATE_ADD(CURDATE(),INTERVAL  3 DAY), 1, 0, 'CONFIRMED',   NULL),
((SELECT id FROM guests WHERE email='alice.martin@mail.com'),  (SELECT id FROM rooms WHERE room_number='501'), (SELECT id FROM users WHERE username='client1'),    'RES-{CURRENT_YEAR}-0014', DATE_ADD(CURDATE(),INTERVAL 10 DAY), DATE_ADD(CURDATE(),INTERVAL 14 DAY), 2, 0, 'CONFIRMED',   'Return guest -- VIP'),
((SELECT id FROM guests WHERE email='ines.dupont@mail.com'),   (SELECT id FROM rooms WHERE room_number='205'), (SELECT id FROM users WHERE username='client9'),    'RES-{CURRENT_YEAR}-0015', DATE_ADD(CURDATE(),INTERVAL  3 DAY), DATE_ADD(CURDATE(),INTERVAL  7 DAY), 2, 2, 'CANCELLED',   'Family room'),
((SELECT id FROM guests WHERE email='bob.jones@mail.com'),     (SELECT id FROM rooms WHERE room_number='403'), (SELECT id FROM users WHERE username='client2'),    'RES-{CURRENT_YEAR}-0016', DATE_ADD(CURDATE(),INTERVAL  1 DAY), DATE_ADD(CURDATE(),INTERVAL  3 DAY), 1, 0, 'CANCELLED',   NULL);
""")

# ─────────────── 13. RESERVATION SERVICES ────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 13. RESERVATION SERVICES (~60% of CHECKED_OUT get services)")
emit("-- ══════════════════════════════════════════════════════════════")

rs_rows = []
for r in room_reservations:
    if r['status'] not in ('CHECKED_OUT', 'CHECKED_IN'):
        continue
    if random.random() > 0.60:
        continue
    rtype    = r['rtype']
    affinity = ROOM_SVC_AFFINITY.get(rtype, [3, 6])
    # 1–4 services; higher-tier rooms get more
    tier_max = {'Single':2,'Double':3,'Twin':2,'Deluxe':3,'Suite':4,'Presidential':5}
    n_svcs   = random.randint(1, tier_max.get(rtype, 2))
    chosen   = random.sample(affinity, min(n_svcs, len(affinity)))
    req_date = r['ci'] + timedelta(days=random.randint(0, max(0, r['nights']-1)))
    req_dt   = rand_time(req_date, 8, 20)
    for svc_idx in chosen:
        _, price, _ = SVCS[svc_idx]
        qty = r['nights'] if svc_idx == 3 else (r['nights'] if svc_idx in (11,12) else 1)
        rs_rows.append(
            f"((SELECT id FROM reservations WHERE reservation_code='{r['code']}'), "
            f"(SELECT id FROM services WHERE code='{SVCS[svc_idx][0]}'), "
            f"{qty}, {price:.2f}, {fdatetime(req_dt)})"
        )

emit(f"-- {len(rs_rows)} reservation_services rows")
for i in range(0, len(rs_rows), BATCH):
    chunk = rs_rows[i:i+BATCH]
    emit("INSERT INTO reservation_services (reservation_id, service_id, quantity, unit_price, requested_at) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# ─────────────── 14. INVOICES + PAYMENTS ─────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 14. INVOICES + INVOICE LINES + PAYMENTS (all CHECKED_OUT)")
emit("-- ══════════════════════════════════════════════════════════════")

inv_counter  = [0]
line_counter = [0]
pay_counter  = [0]

inv_rows  = []
line_rows = []
pay_rows  = []

def next_inv():
    inv_counter[0] += 1
    return f'INV-HIST-{inv_counter[0]:05d}'

for r in room_reservations:
    if r['status'] != 'CHECKED_OUT':
        continue

    code     = r['code']
    rtype    = r['rtype']
    nights   = r['nights']
    npp      = ROOM_PRICE[rtype]
    room_sub = npp * nights
    issued   = rand_time(r['co'], 9, 17)

    # Collect services for this reservation
    svc_sub = 0.0
    # recompute from rs_rows isn't easy here, so approximate from same logic
    # (we use a deterministic seed so re-running gives same results)
    # Instead, just use a realistic add-on fraction
    addon_frac = {'Single':0.05,'Double':0.08,'Twin':0.06,
                  'Deluxe':0.12,'Suite':0.20,'Presidential':0.35}
    svc_sub = round(room_sub * addon_frac[rtype] * random.uniform(0.5, 1.5), 2)
    subtotal = round(room_sub + svc_sub, 2)
    tax      = round(subtotal * 0.10, 2)
    total    = round(subtotal + tax, 2)

    inv_no   = next_inv()
    status   = 'PAID'
    method   = random.choices(['CASH','CARD','TRANSFER'],[25,55,20])[0]
    txn      = f"'TXN-{method[:2]}-{inv_counter[0]:05d}'" if method != 'CASH' else 'NULL'
    paid_dt  = rand_time(r['co'], 10, 18)

    inv_rows.append(
        f"((SELECT id FROM reservations WHERE reservation_code='{code}'), "
        f"'{inv_no}', {fdatetime(issued)}, "
        f"{subtotal:.2f}, {tax:.2f}, {total:.2f}, 'PAID', NULL)"
    )

    # Invoice lines
    line_rows.append(
        f"((SELECT id FROM invoices WHERE invoice_number='{inv_no}'), "
        f"'ROOM', (SELECT id FROM rooms WHERE room_number='{r['room_no']}'), "
        f"'{rtype} room — {nights} night(s) @ {npp:.0f} MAD', "
        f"{nights}, {npp:.2f}, {room_sub:.2f})"
    )
    if svc_sub > 0:
        line_rows.append(
            f"((SELECT id FROM invoices WHERE invoice_number='{inv_no}'), "
            f"'SERVICE', NULL, 'Additional services', "
            f"1, {svc_sub:.2f}, {svc_sub:.2f})"
        )
    if tax > 0:
        line_rows.append(
            f"((SELECT id FROM invoices WHERE invoice_number='{inv_no}'), "
            f"'OTHER', NULL, 'VAT 10%', "
            f"1, {tax:.2f}, {tax:.2f})"
        )

    pay_rows.append(
        f"((SELECT id FROM invoices WHERE invoice_number='{inv_no}'), "
        f"{total:.2f}, '{method}', {fdatetime(paid_dt)}, {txn}, 'SUCCESS')"
    )

emit(f"-- {len(inv_rows)} invoices")
for i in range(0, len(inv_rows), BATCH):
    chunk = inv_rows[i:i+BATCH]
    emit("INSERT INTO invoices (reservation_id, invoice_number, issued_at, subtotal_amount, tax_amount, total_amount, status, notes) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

emit(f"-- {len(line_rows)} invoice lines")
for i in range(0, len(line_rows), BATCH):
    chunk = line_rows[i:i+BATCH]
    emit("INSERT INTO invoice_lines (invoice_id, line_type, reference_id, description, quantity, unit_price, line_total) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

emit(f"-- {len(pay_rows)} payments")
for i in range(0, len(pay_rows), BATCH):
    chunk = pay_rows[i:i+BATCH]
    emit("INSERT INTO payments (invoice_id, amount, method, paid_at, transaction_ref, status) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# Seed invoices / payments (original 5 paid ones)
emit("-- Seed invoices (original paid stays)")
emit(f"""INSERT INTO invoices (reservation_id, invoice_number, issued_at, subtotal_amount, tax_amount, total_amount, status, notes) VALUES
((SELECT id FROM reservations WHERE reservation_code='RES-{CURRENT_YEAR}-0006'), 'INV-SEED-0001', DATE_SUB(NOW(),INTERVAL 2 DAY),  1305.00, 130.50, 1435.50, 'ISSUED', NULL),
((SELECT id FROM reservations WHERE reservation_code='RES-{CURRENT_YEAR}-0007'), 'INV-SEED-0002', DATE_SUB(NOW(),INTERVAL 1 DAY),  1650.00, 165.00, 1815.00, 'ISSUED', NULL),
((SELECT id FROM reservations WHERE reservation_code='RES-{CURRENT_YEAR}-0008'), 'INV-SEED-0003', DATE_SUB(NOW(),INTERVAL 3 DAY),  2850.00, 285.00, 3135.00, 'ISSUED', 'City view supplement'),
((SELECT id FROM reservations WHERE reservation_code='RES-{CURRENT_YEAR}-0009'), 'INV-SEED-0004', NOW(),                            2400.00, 240.00, 2640.00, 'ISSUED', 'VIP late checkout'),
((SELECT id FROM reservations WHERE reservation_code='RES-{CURRENT_YEAR}-0010'), 'INV-SEED-0005', DATE_SUB(NOW(),INTERVAL 5 DAY), 12500.00,1250.00,13750.00, 'ISSUED', 'Presidential long-stay');
""")

# ─────────────── 15. CLEANING REQUESTS (historical) ──────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 15. CLEANING REQUESTS (one per CHECKED_OUT reservation)")
emit("-- ══════════════════════════════════════════════════════════════")

cr_rows = []
for r in room_reservations:
    if r['status'] != 'CHECKED_OUT':
        continue
    if random.random() > 0.85:   # 85% of checkouts trigger a cleaning request
        continue
    pri  = random.choices(['LOW','MEDIUM','HIGH','URGENT'],[10,50,30,10])[0]
    note = random.choice(CLEANING_NOTES)
    req_dt   = rand_time(r['co'], 8, 14)
    start_dt = rand_time(r['co'], 10, 16)
    end_dt   = datetime(start_dt.year, start_dt.month, start_dt.day,
                        start_dt.hour + random.randint(1,3), random.randint(0,59))
    clner = random.choice(['EMP-CLN-001','EMP-CLN-002'])

    cr_rows.append(
        f"((SELECT id FROM rooms WHERE room_number='{r['room_no']}'), "
        f"(SELECT id FROM users WHERE username='reception1'), "
        f"(SELECT id FROM staff_profiles WHERE employee_code='{clner}'), "
        f"(SELECT id FROM reservations WHERE reservation_code='{r['code']}'), "
        f"'{pri}', 'DONE', '{note}', "
        f"{fdatetime(req_dt)}, {fdatetime(start_dt)}, {fdatetime(end_dt)})"
    )

emit(f"-- {len(cr_rows)} cleaning requests")
for i in range(0, len(cr_rows), BATCH):
    chunk = cr_rows[i:i+BATCH]
    emit("INSERT INTO cleaning_requests (room_id, requested_by_user_id, assigned_to_staff_id, reservation_id, priority, status, request_note, created_at, started_at, completed_at) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# Seed current cleaning requests
emit(f"""-- Seed current cleaning requests (matching live room statuses)
INSERT INTO cleaning_requests (room_id, requested_by_user_id, assigned_to_staff_id, reservation_id, priority, status, request_note, created_at, started_at, completed_at) VALUES
((SELECT id FROM rooms WHERE room_number='104'),
 (SELECT id FROM users WHERE username='reception2'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'),
 NULL, 'MEDIUM', 'IN_PROGRESS', 'Guest requested extra towels',
 NOW(), NOW(), NULL),
((SELECT id FROM rooms WHERE room_number='304'),
 (SELECT id FROM users WHERE username='reception1'),
 NULL, NULL, 'HIGH', 'NEW', 'VIP arrival tonight -- must be perfect',
 NOW(), NULL, NULL),
((SELECT id FROM rooms WHERE room_number='102'),
 (SELECT id FROM users WHERE username='reception2'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'),
 (SELECT id FROM reservations WHERE reservation_code='RES-{CURRENT_YEAR}-0006'),
 'MEDIUM', 'NEW', 'Daily service', NOW(), NULL, NULL);
""")

# ─────────────── 16. MAINTENANCE REQUESTS (historical) ───────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 16. MAINTENANCE REQUESTS (~1 per month per room, realistic mix)")
emit("-- ══════════════════════════════════════════════════════════════")

mr_rows = []
room_nos = [r[0] for r in ROOMS]
cur = START_DATE
while cur <= END_DATE:
    # Each month, ~40% chance a random room gets a maintenance request
    for rno in room_nos:
        if random.random() > 0.30:
            continue
        ti, desc, pri = random.choice(MAINT_POOL)
        req_dt = rand_time(cur, 7, 20)
        assignee = random.choice(['EMP-MNT-001','EMP-MNT-002','EMP-MNT-001'])  # MNT-001 more common
        resolved = random.random() < 0.90  # 90% resolved
        if resolved:
            res_dt = fdatetime(rand_time(add_days(cur, random.randint(0,3)), 8, 17))
            status = 'RESOLVED'
        else:
            res_dt = 'NULL'
            status = random.choice(['IN_PROGRESS','NEW'])

        reporter = random.choice(['reception1','reception2','cleaning1','cleaning1'])
        ti_esc   = ti.replace("'", "\\'")
        desc_esc = desc.replace("'", "\\'")

        mr_rows.append(
            f"((SELECT id FROM rooms WHERE room_number='{rno}'), "
            f"(SELECT id FROM users WHERE username='{reporter}'), "
            f"(SELECT id FROM staff_profiles WHERE employee_code='{assignee}'), "
            f"'{pri}', '{status}', '{ti_esc}', '{desc_esc}', "
            f"{fdatetime(req_dt)}, {res_dt})"
        )
    cur = add_days(cur, 30)  # advance roughly 1 month

emit(f"-- {len(mr_rows)} maintenance requests")
for i in range(0, len(mr_rows), BATCH):
    chunk = mr_rows[i:i+BATCH]
    emit("INSERT INTO maintenance_requests (room_id, reported_by_user_id, assigned_to_staff_id, priority, status, title, description, created_at, resolved_at) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# Seed current maintenance
emit("""-- Seed current maintenance requests
INSERT INTO maintenance_requests (room_id, reported_by_user_id, assigned_to_staff_id, priority, status, title, description, created_at, resolved_at) VALUES
((SELECT id FROM rooms WHERE room_number='204'),
 (SELECT id FROM users WHERE username='cleaning1'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'),
 'MEDIUM', 'IN_PROGRESS', 'Shower drain blocked',
 'Shower drain blocked -- slow drainage.',
 DATE_SUB(NOW(),INTERVAL 1 DAY), NULL),
((SELECT id FROM rooms WHERE room_number='403'),
 (SELECT id FROM users WHERE username='reception2'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'),
 'URGENT', 'NEW', 'AC compressor fault',
 'AC compressor fault -- complete unit replacement needed.',
 NOW(), NULL),
((SELECT id FROM rooms WHERE room_number='301'),
 (SELECT id FROM users WHERE username='cleaning2'),
 NULL, 'MEDIUM', 'NEW', 'Bedside lamp flickering',
 'Bedside lamp flickering -- probable loose wire.',
 NOW(), NULL);
""")

# ─────────────── 17. PAYROLL (36 months × 6 staff) ───────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- 17. PAYROLL (monthly for 3 years × 6 staff = 216 records)")
emit("-- ══════════════════════════════════════════════════════════════")

pr_rows = []
for y, m in month_iter(START_DATE, END_DATE):
    p_start = date(y, m, 1)
    p_end   = last_day_of(y, m)
    paid_dt = datetime(y, m if m < 12 else 12, min(5, p_end.day), 10, 0)
    if p_end >= TODAY:
        continue   # don't generate unpaid future months

    for emp in STAFF_CODES:
        base  = STAFF_BASE[emp]
        # Performance bonuses: higher in summer/Dec, modest otherwise
        mo = p_start.month
        if mo in (7, 8, 12):   bonus = round(base * random.uniform(0.08, 0.15), 2)
        elif mo in (6, 9):     bonus = round(base * random.uniform(0.04, 0.09), 2)
        else:                  bonus = round(base * random.uniform(0.00, 0.04), 2)

        # Deductions: CNSS ~6.74%, CIMR optional
        ded = round(base * 0.0674 + random.choice([0,0,0, base*0.02]), 2)
        net = round(base + bonus - ded, 2)

        pr_rows.append(
            f"((SELECT id FROM staff_profiles WHERE employee_code='{emp}'), "
            f"{fdate(p_start)}, {fdate(p_end)}, "
            f"{base:.2f}, {bonus:.2f}, {ded:.2f}, {net:.2f}, "
            f"'PAID', {fdatetime(paid_dt)})"
        )

emit(f"-- {len(pr_rows)} payroll records")
for i in range(0, len(pr_rows), BATCH):
    chunk = pr_rows[i:i+BATCH]
    emit("INSERT INTO payroll (staff_profile_id, period_start, period_end, base_salary, bonuses, deductions, net_salary, status, paid_at) VALUES")
    emit(',\n'.join(chunk) + ';')
    emit()

# Current month payroll (GENERATED status)
emit("""-- Current month payroll (generated, not yet paid)
INSERT INTO payroll (staff_profile_id, period_start, period_end, base_salary, bonuses, deductions, net_salary, status, paid_at) VALUES
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-001'), DATE_FORMAT(CURDATE(),'%Y-%m-01'), LAST_DAY(CURDATE()), 5200.00, 500.00, 350.58, 5349.42, 'GENERATED', NULL),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-002'), DATE_FORMAT(CURDATE(),'%Y-%m-01'), LAST_DAY(CURDATE()), 4800.00,   0.00, 323.52, 4476.48, 'GENERATED', NULL),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'), DATE_FORMAT(CURDATE(),'%Y-%m-01'), LAST_DAY(CURDATE()), 3900.00, 200.00, 262.86, 3837.14, 'GENERATED', NULL),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-002'), DATE_FORMAT(CURDATE(),'%Y-%m-01'), LAST_DAY(CURDATE()), 3700.00,   0.00, 249.38, 3450.62, 'GENERATED', NULL),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'), DATE_FORMAT(CURDATE(),'%Y-%m-01'), LAST_DAY(CURDATE()), 4600.00, 300.00, 310.04, 4589.96, 'GENERATED', NULL),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-002'), DATE_FORMAT(CURDATE(),'%Y-%m-01'), LAST_DAY(CURDATE()), 4400.00,   0.00, 296.56, 4103.44, 'GENERATED', NULL);
""")

# ─────────────── VERIFY ───────────────────────────────────────────────────────
emit("-- ══════════════════════════════════════════════════════════════")
emit("-- VERIFY COUNTS")
emit("-- ══════════════════════════════════════════════════════════════")
emit("""SELECT 'users'            AS tbl, COUNT(*) AS n FROM users
UNION ALL SELECT 'guests',              COUNT(*) FROM guests
UNION ALL SELECT 'rooms',               COUNT(*) FROM rooms
UNION ALL SELECT 'reservations',        COUNT(*) FROM reservations
UNION ALL SELECT 'reservation_services',COUNT(*) FROM reservation_services
UNION ALL SELECT 'invoices',            COUNT(*) FROM invoices
UNION ALL SELECT 'invoice_lines',       COUNT(*) FROM invoice_lines
UNION ALL SELECT 'payments',            COUNT(*) FROM payments
UNION ALL SELECT 'cleaning_requests',   COUNT(*) FROM cleaning_requests
UNION ALL SELECT 'maintenance_requests',COUNT(*) FROM maintenance_requests
UNION ALL SELECT 'staff_shift_assignments', COUNT(*) FROM staff_shift_assignments
UNION ALL SELECT 'staff_attendance',    COUNT(*) FROM staff_attendance
UNION ALL SELECT 'payroll',             COUNT(*) FROM payroll;
""")

# ── revenue summary for quick sanity check ────────────────────────────────────
emit("""-- Quick revenue check by year
SELECT YEAR(issued_at) AS yr, COUNT(*) AS invoices,
       FORMAT(SUM(total_amount),2) AS total_revenue_MAD
FROM invoices WHERE status='PAID'
GROUP BY yr ORDER BY yr;
""")

# ══════════════════════════════════════════════════════════════════════════════
# AUTO-GENERATE seed.sql IN SAME DIRECTORY
# ══════════════════════════════════════════════════════════════════════════════
import os
script_dir = os.path.dirname(os.path.abspath(__file__))
seed_file = os.path.join(script_dir, 'seed.sql')

with open(seed_file, 'w', encoding='utf-8') as f:
    f.write('\n'.join(out))

print(f"✓ seed.sql generated")
print(f"  Location: {seed_file}")
print(f"  Date range: {START_DATE} → {END_DATE}")
print(f"  Reservations: {len(res_rows)}")
