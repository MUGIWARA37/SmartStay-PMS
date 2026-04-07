PRAGMA foreign_keys = ON;

-- =========================================
-- USERS & STAFF
-- =========================================

CREATE TABLE users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  username      TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role          TEXT NOT NULL CHECK (role IN ('CLIENT','STAFF','ADMIN')),
  first_name    TEXT NOT NULL,
  last_name     TEXT NOT NULL,
  email         TEXT,
  phone         TEXT,
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);

PRAGMA foreign_keys = ON;

-- 10 master security questions
CREATE TABLE security_questions (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  question_code TEXT NOT NULL UNIQUE,   -- e.g. Q01
  question_text TEXT NOT NULL,
  active        INTEGER NOT NULL DEFAULT 1
);

-- Each user selects answers for all 10 questions (recommended for your requirement)
-- Store only answer_hash, never plain answer
CREATE TABLE user_security_answers (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id         INTEGER NOT NULL,
  question_id     INTEGER NOT NULL,
  answer_hash     TEXT NOT NULL,        -- hashed answer (e.g. bcrypt/argon2)
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, question_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES security_questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_sec_answers_user ON user_security_answers(user_id);

-- Password reset session/challenge:
-- system picks 3 random question_ids from user's answered questions
CREATE TABLE password_reset_challenges (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id          INTEGER NOT NULL,
  token            TEXT NOT NULL UNIQUE,    -- random reset token/session id
  question1_id     INTEGER NOT NULL,
  question2_id     INTEGER NOT NULL,
  question3_id     INTEGER NOT NULL,
  attempts_used    INTEGER NOT NULL DEFAULT 0,
  max_attempts     INTEGER NOT NULL DEFAULT 3,
  verified         INTEGER NOT NULL DEFAULT 0,
  expires_at       DATETIME NOT NULL,       -- short expiry (e.g. 15 min)
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (question1_id) REFERENCES security_questions(id) ON DELETE RESTRICT,
  FOREIGN KEY (question2_id) REFERENCES security_questions(id) ON DELETE RESTRICT,
  FOREIGN KEY (question3_id) REFERENCES security_questions(id) ON DELETE RESTRICT,
  CHECK (question1_id <> question2_id),
  CHECK (question1_id <> question3_id),
  CHECK (question2_id <> question3_id)
);

CREATE INDEX idx_reset_challenges_user ON password_reset_challenges(user_id);
CREATE INDEX idx_reset_challenges_expires ON password_reset_challenges(expires_at);

-- Optional audit log for recovery attempts
CREATE TABLE password_reset_audit (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  challenge_id   INTEGER NOT NULL,
  user_id        INTEGER NOT NULL,
  success        INTEGER NOT NULL,       -- 1 success, 0 fail
  ip_address     TEXT,
  user_agent     TEXT,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (challenge_id) REFERENCES password_reset_challenges(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_reset_audit_user_time ON password_reset_audit(user_id, created_at);

-- Staff profile (no payroll rates/hours here)
CREATE TABLE staff_profiles (
  user_id    INTEGER PRIMARY KEY,
  position   TEXT,
  hire_date  DATE,
  active     INTEGER NOT NULL DEFAULT 1,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================
-- SHIFTS / ATTENDANCE
-- =========================================

CREATE TABLE shifts (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  name       TEXT NOT NULL UNIQUE CHECK (name IN ('MORNING','MIDDAY','NIGHT')),
  start_time TIME NOT NULL,
  end_time   TIME NOT NULL
);

CREATE TABLE staff_shift_assignments (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  staff_id       INTEGER NOT NULL,
  shift_id       INTEGER NOT NULL,
  effective_from DATE NOT NULL,
  effective_to   DATE,
  FOREIGN KEY (staff_id) REFERENCES staff_profiles(user_id) ON DELETE CASCADE,
  FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE RESTRICT
);

CREATE TABLE staff_attendance (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  staff_id   INTEGER NOT NULL,
  shift_id   INTEGER NOT NULL,
  shift_date DATE NOT NULL,
  check_in   DATETIME,
  check_out  DATETIME,
  was_late   INTEGER NOT NULL DEFAULT 0,
  notes      TEXT,
  FOREIGN KEY (staff_id) REFERENCES staff_profiles(user_id) ON DELETE CASCADE,
  FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE RESTRICT
);

-- =========================================
-- MONTHLY PAYROLL (self-contained snapshot)
-- Formula:
-- total_salary = (regular_hours * hourly_rate) + (overtime_hours * overtime_rate)
-- =========================================

CREATE TABLE payroll (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  staff_id         INTEGER NOT NULL,
  year_month       TEXT NOT NULL, -- YYYY-MM (e.g., 2026-04)
  regular_hours    DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (regular_hours >= 0),
  overtime_hours   DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (overtime_hours >= 0),
  hourly_rate      DECIMAL(10,2) NOT NULL CHECK (hourly_rate >= 0),
  overtime_rate    DECIMAL(10,2) NOT NULL CHECK (overtime_rate >= 0),
  total_salary     DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (total_salary >= 0),
  status           TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','APPROVED','PAID')),
  paid_at          DATETIME,
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(staff_id, year_month),
  CHECK (year_month GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]'),
  CHECK (CAST(SUBSTR(year_month, 6, 2) AS INTEGER) BETWEEN 1 AND 12),
  FOREIGN KEY (staff_id) REFERENCES staff_profiles(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_payroll_staff_month ON payroll(staff_id, year_month);
CREATE INDEX idx_payroll_month ON payroll(year_month);

-- Auto-calculate total_salary before insert
CREATE TRIGGER trg_payroll_calc_before_insert
BEFORE INSERT ON payroll
FOR EACH ROW
BEGIN
  SELECT
    NEW.total_salary =
      ((NEW.regular_hours * NEW.hourly_rate) +
       (NEW.overtime_hours * NEW.overtime_rate)),
    NEW.updated_at = CURRENT_TIMESTAMP;
END;

-- Auto-calculate total_salary before relevant payroll updates
CREATE TRIGGER trg_payroll_calc_before_update
BEFORE UPDATE OF regular_hours, overtime_hours, hourly_rate, overtime_rate
ON payroll
FOR EACH ROW
BEGIN
  SELECT
    NEW.total_salary =
      ((NEW.regular_hours * NEW.hourly_rate) +
       (NEW.overtime_hours * NEW.overtime_rate)),
    NEW.updated_at = CURRENT_TIMESTAMP;
END;

-- Keep updated_at fresh for any other UPDATE statements
CREATE TRIGGER trg_payroll_touch_updated_at
AFTER UPDATE ON payroll
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
  UPDATE payroll
  SET updated_at = CURRENT_TIMESTAMP
  WHERE id = NEW.id;
END;

-- Annual salary aggregation for admin
CREATE VIEW annual_salary_by_employee AS
SELECT
  p.staff_id,
  SUBSTR(p.year_month, 1, 4) AS year,
  ROUND(SUM(p.total_salary), 2) AS annual_salary
FROM payroll p
GROUP BY p.staff_id, SUBSTR(p.year_month, 1, 4);

-- Optional detailed annual view with employee names
CREATE VIEW annual_salary_with_employee AS
SELECT
  p.staff_id,
  u.username,
  u.first_name,
  u.last_name,
  SUBSTR(p.year_month, 1, 4) AS year,
  ROUND(SUM(p.total_salary), 2) AS annual_salary
FROM payroll p
JOIN users u ON u.id = p.staff_id
GROUP BY p.staff_id, u.username, u.first_name, u.last_name, SUBSTR(p.year_month, 1, 4);

-- =========================================
-- ROOMS / GUESTS / RESERVATIONS
-- =========================================

CREATE TABLE room_types (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT NOT NULL UNIQUE,
  base_rate     DECIMAL(10,2) NOT NULL CHECK (base_rate >= 0),
  max_occupancy INTEGER NOT NULL CHECK (max_occupancy > 0),
  description   TEXT
);

CREATE TABLE rooms (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  room_number   TEXT NOT NULL UNIQUE,
  room_type_id  INTEGER NOT NULL,
  status        TEXT NOT NULL CHECK (status IN ('AVAILABLE','OCCUPIED','OUT_OF_SERVICE')),
  floor         INTEGER,
  notes         TEXT,
  FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE RESTRICT
);

CREATE TABLE guests (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id    INTEGER, -- nullable for walk-ins
  first_name TEXT NOT NULL,
  last_name  TEXT NOT NULL,
  email      TEXT,
  phone      TEXT,
  doc_id     TEXT, -- passport / national ID / driving license
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE reservations (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  guest_id       INTEGER NOT NULL,
  room_id        INTEGER NOT NULL,
  check_in_date  DATE NOT NULL,
  check_out_date DATE NOT NULL,
  status         TEXT NOT NULL CHECK (status IN ('BOOKED','CHECKED_IN','CHECKED_OUT','CANCELLED')),
  adults         INTEGER NOT NULL DEFAULT 1 CHECK (adults >= 1),
  children       INTEGER NOT NULL DEFAULT 0 CHECK (children >= 0),
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  CHECK (check_out_date > check_in_date),
  FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE RESTRICT,
  FOREIGN KEY (room_id)  REFERENCES rooms(id)  ON DELETE RESTRICT
);

CREATE INDEX idx_res_room_dates ON reservations(room_id, check_in_date, check_out_date);

-- =========================================
-- BILLING
-- =========================================

CREATE TABLE invoices (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  reservation_id INTEGER NOT NULL,
  guest_id       INTEGER NOT NULL,
  issue_date     DATE NOT NULL,
  due_date       DATE,
  total_amount   DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
  currency       TEXT NOT NULL DEFAULT 'USD',
  status         TEXT NOT NULL CHECK (status IN ('DRAFT','ISSUED','PAID','VOID')),
  FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE RESTRICT,
  FOREIGN KEY (guest_id)       REFERENCES guests(id)        ON DELETE RESTRICT
);

CREATE TABLE invoice_lines (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  invoice_id  INTEGER NOT NULL,
  description TEXT NOT NULL,
  quantity    DECIMAL(10,2) NOT NULL DEFAULT 1 CHECK (quantity > 0),
  unit_price  DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
  line_total  DECIMAL(10,2) NOT NULL CHECK (line_total >= 0),
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE TABLE payments (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  invoice_id INTEGER NOT NULL,
  amount     DECIMAL(10,2) NOT NULL CHECK (amount > 0),
  method     TEXT NOT NULL CHECK (method IN ('CASH','CARD','ONLINE')),
  paid_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reference  TEXT, -- transaction / receipt / gateway reference
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- =========================================
-- SERVICES / EXTRAS
-- =========================================

CREATE TABLE services (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT NOT NULL UNIQUE,
  default_price DECIMAL(10,2) NOT NULL CHECK (default_price >= 0),
  type          TEXT NOT NULL DEFAULT 'OTHER'
);

CREATE TABLE reservation_services (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  reservation_id INTEGER NOT NULL,
  service_id     INTEGER NOT NULL,
  quantity       DECIMAL(10,2) NOT NULL DEFAULT 1 CHECK (quantity > 0),
  unit_price     DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
  line_total     DECIMAL(10,2) NOT NULL CHECK (line_total >= 0),
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
  FOREIGN KEY (service_id)     REFERENCES services(id)     ON DELETE RESTRICT
);