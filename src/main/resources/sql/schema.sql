PRAGMA foreign_keys = ON;

-- Users (CLIENT, STAFF, ADMIN)
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

-- Staff-only details (1:1 with users when role=STAFF)
CREATE TABLE staff_profiles (
  user_id    INTEGER PRIMARY KEY,
  position   TEXT,
  hire_date  DATE,
  active     INTEGER NOT NULL DEFAULT 1,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Shift templates (adjust times here to change future schedules)
CREATE TABLE shifts (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  name       TEXT NOT NULL UNIQUE CHECK (name IN ('MORNING','MIDDAY','NIGHT')),
  start_time TIME NOT NULL,
  end_time   TIME NOT NULL
);

-- Shift assignments (who works which shift over a date range)
CREATE TABLE staff_shift_assignments (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  staff_id       INTEGER NOT NULL,
  shift_id       INTEGER NOT NULL,
  effective_from DATE NOT NULL,
  effective_to   DATE,
  FOREIGN KEY (staff_id) REFERENCES staff_profiles(user_id) ON DELETE CASCADE,
  FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE RESTRICT
);

-- Attendance / lateness log
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

-- Room types and rooms
CREATE TABLE room_types (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT NOT NULL UNIQUE,
  base_rate     DECIMAL(10,2) NOT NULL,
  max_occupancy INTEGER NOT NULL,
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

-- Guests (app clients or walk-ins)
CREATE TABLE guests (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id    INTEGER, -- nullable: walk-ins may not have an app account
  first_name TEXT NOT NULL,
  last_name  TEXT NOT NULL,
  email      TEXT,
  phone      TEXT,
  doc_id     TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Reservations
CREATE TABLE reservations (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  guest_id       INTEGER NOT NULL,
  room_id        INTEGER NOT NULL,
  check_in_date  DATE NOT NULL,
  check_out_date DATE NOT NULL,
  status         TEXT NOT NULL CHECK (status IN ('BOOKED','CHECKED_IN','CHECKED_OUT','CANCELLED')),
  adults         INTEGER NOT NULL DEFAULT 1,
  children       INTEGER NOT NULL DEFAULT 0,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE RESTRICT,
  FOREIGN KEY (room_id)  REFERENCES rooms(id)  ON DELETE RESTRICT
);

CREATE INDEX idx_res_room_dates ON reservations(room_id, check_in_date, check_out_date);

-- Billing
CREATE TABLE invoices (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  reservation_id INTEGER NOT NULL,
  guest_id       INTEGER NOT NULL,
  issue_date     DATE NOT NULL,
  due_date       DATE,
  total_amount   DECIMAL(10,2) NOT NULL,
  currency       TEXT NOT NULL DEFAULT 'USD',
  status         TEXT NOT NULL CHECK (status IN ('DRAFT','ISSUED','PAID','VOID')),
  FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE RESTRICT,
  FOREIGN KEY (guest_id)       REFERENCES guests(id)        ON DELETE RESTRICT
);

CREATE TABLE invoice_lines (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  invoice_id  INTEGER NOT NULL,
  description TEXT NOT NULL,
  quantity    DECIMAL(10,2) NOT NULL DEFAULT 1,
  unit_price  DECIMAL(10,2) NOT NULL,
  line_total  DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE TABLE payments (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  invoice_id INTEGER NOT NULL,
  amount     DECIMAL(10,2) NOT NULL,
  method     TEXT NOT NULL CHECK (method IN ('CASH','CARD','ONLINE')),
  paid_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reference  TEXT,
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- Extras / services
CREATE TABLE services (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT NOT NULL UNIQUE,
  default_price DECIMAL(10,2) NOT NULL,
  type          TEXT NOT NULL DEFAULT 'OTHER' -- e.g., MINIBAR/SPA/LAUNDRY
);

CREATE TABLE reservation_services (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  reservation_id INTEGER NOT NULL,
  service_id     INTEGER NOT NULL,
  quantity       DECIMAL(10,2) NOT NULL DEFAULT 1,
  unit_price     DECIMAL(10,2) NOT NULL,
  line_total     DECIMAL(10,2) NOT NULL,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
  FOREIGN KEY (service_id)     REFERENCES services(id)     ON DELETE RESTRICT
);