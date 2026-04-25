-- DB (Docker): Smart_Hotel_v_1_0

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- For clean re-run in dev:
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- DROP VIEWS
-- =========================
DROP VIEW IF EXISTS annual_salary_with_employee;
DROP VIEW IF EXISTS annual_salary_by_employee;

-- =========================
-- DROP TABLES (reverse dependency order)
-- =========================
DROP TABLE IF EXISTS password_reset_audit;
DROP TABLE IF EXISTS password_reset_challenges;
DROP TABLE IF EXISTS user_security_answers;
DROP TABLE IF EXISTS security_questions;

DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS invoice_lines;
DROP TABLE IF EXISTS invoices;

DROP TABLE IF EXISTS reservation_services;
DROP TABLE IF EXISTS services;

DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS guests;

DROP TABLE IF EXISTS cleaning_requests;
DROP TABLE IF EXISTS maintenance_requests;

DROP TABLE IF EXISTS payroll;
DROP TABLE IF EXISTS staff_attendance;
DROP TABLE IF EXISTS staff_shift_assignments;
DROP TABLE IF EXISTS shifts;
DROP TABLE IF EXISTS staff_profiles;

DROP TABLE IF EXISTS room_images;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS room_types;

DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================
-- USERS / AUTH
-- =========================
CREATE TABLE users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL,
  email VARCHAR(120) NULL,
  password_hash VARCHAR(255) NOT NULL,

  -- Keep staff type in staff_profiles.position (cleaning/maintenance/reception)
  role ENUM('CLIENT','STAFF','ADMIN') NOT NULL DEFAULT 'CLIENT',

  is_active TINYINT(1) NOT NULL DEFAULT 1,
  last_login_at DATETIME NULL,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE staff_profiles (
  user_id BIGINT UNSIGNED NOT NULL,

  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  phone VARCHAR(30) NULL,
  cin VARCHAR(30) NULL,

  position ENUM('cleaning','maintenance','reception') NOT NULL,

  -- Salary model: hourly + overtime per hour
  hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  overtime_hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,

  hired_at DATE NULL,
  terminated_at DATE NULL,

  PRIMARY KEY (user_id),
  UNIQUE KEY uk_staff_profiles_cin (cin),

  CONSTRAINT fk_staff_profiles_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT chk_staff_hourly_rate CHECK (hourly_rate >= 0),
  CONSTRAINT chk_staff_overtime_rate CHECK (overtime_hourly_rate >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================
-- ROOMS
-- =========================
CREATE TABLE room_types (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  description TEXT NULL,
  base_price_per_night DECIMAL(10,2) NOT NULL,
  capacity_adults INT NOT NULL DEFAULT 2,
  capacity_children INT NOT NULL DEFAULT 0,
  bed_type VARCHAR(60) NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,

  PRIMARY KEY (id),
  UNIQUE KEY uk_room_types_name (name),
  CONSTRAINT chk_room_type_price CHECK (base_price_per_night >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rooms (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  room_number VARCHAR(20) NOT NULL,
  floor INT NOT NULL DEFAULT 0,
  room_type_id BIGINT UNSIGNED NOT NULL,

  status ENUM('AVAILABLE','OCCUPIED','DIRTY','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
  notes VARCHAR(255) NULL,

  PRIMARY KEY (id),
  UNIQUE KEY uk_rooms_room_number (room_number),
  KEY idx_rooms_type (room_type_id),
  KEY idx_rooms_status (status),

  CONSTRAINT fk_rooms_room_type
    FOREIGN KEY (room_type_id) REFERENCES room_types(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Multiple images per room (for slideshow)
CREATE TABLE room_images (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  room_id BIGINT UNSIGNED NOT NULL,

  -- Store relative resource path or URL
  -- e.g. /images/rooms/101/1.jpg
  image_path VARCHAR(255) NOT NULL,

  alt_text VARCHAR(120) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_primary TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_room_images_unique (room_id, image_path),
  KEY idx_room_images_room_sort (room_id, sort_order),

  CONSTRAINT fk_room_images_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================
-- GUESTS & RESERVATIONS
-- =========================
CREATE TABLE guests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL, -- optional link to CLIENT user

  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  email VARCHAR(120) NULL,
  phone VARCHAR(30) NULL,
  cin VARCHAR(30) NULL, -- ID document
  address VARCHAR(255) NULL,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_guests_cin (cin),
  KEY idx_guests_user (user_id),

  CONSTRAINT fk_guests_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reservations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reservation_code VARCHAR(30) NOT NULL,

  guest_id BIGINT UNSIGNED NOT NULL,
  room_id BIGINT UNSIGNED NOT NULL,

  check_in_date DATE NOT NULL,
  check_out_date DATE NOT NULL,

  status ENUM('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'PENDING',

  adults INT NOT NULL DEFAULT 1,
  children INT NOT NULL DEFAULT 0,

  created_by_user_id BIGINT UNSIGNED NULL, -- staff/admin who created it (optional)
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_reservations_code (reservation_code),

  KEY idx_reservations_guest (guest_id),
  KEY idx_reservations_room (room_id),
  KEY idx_reservations_dates (check_in_date, check_out_date),

  CONSTRAINT fk_reservations_guest
    FOREIGN KEY (guest_id) REFERENCES guests(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  CONSTRAINT fk_reservations_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  CONSTRAINT fk_reservations_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE,

  CONSTRAINT chk_reservation_dates CHECK (check_out_date > check_in_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================
-- SERVICES (add-ons)
-- =========================
CREATE TABLE services (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  description TEXT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  is_active TINYINT(1) NOT NULL DEFAULT 1,

  PRIMARY KEY (id),
  UNIQUE KEY uk_services_name (name),
  CONSTRAINT chk_services_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reservation_services (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reservation_id BIGINT UNSIGNED NOT NULL,
  service_id BIGINT UNSIGNED NOT NULL,

  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL, -- snapshot at booking time

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_res_service_unique (reservation_id, service_id),
  KEY idx_res_services_reservation (reservation_id),
  KEY idx_res_services_service (service_id),

  CONSTRAINT fk_res_services_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT fk_res_services_service
    FOREIGN KEY (service_id) REFERENCES services(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  CONSTRAINT chk_res_services_qty CHECK (quantity > 0),
  CONSTRAINT chk_res_services_unit_price CHECK (unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================
-- BILLING / INVOICES / PAYMENTS
-- =========================
CREATE TABLE invoices (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_number VARCHAR(30) NOT NULL,
  reservation_id BIGINT UNSIGNED NOT NULL,

  issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('DRAFT','ISSUED','PAID','CANCELLED') NOT NULL DEFAULT 'ISSUED',
  currency CHAR(3) NOT NULL DEFAULT 'MAD',

  subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  tax DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total DECIMAL(10,2) NOT NULL DEFAULT 0.00,

  PRIMARY KEY (id),
  UNIQUE KEY uk_invoices_number (invoice_number),
  UNIQUE KEY uk_invoices_reservation (reservation_id),

  CONSTRAINT fk_invoices_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  CONSTRAINT chk_invoice_amounts CHECK (subtotal >= 0 AND tax >= 0 AND discount >= 0 AND total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE invoice_lines (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT UNSIGNED NOT NULL,

  line_type ENUM('ROOM_NIGHT','SERVICE','OTHER') NOT NULL,
  description VARCHAR(255) NOT NULL,

  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  line_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,

  PRIMARY KEY (id),
  KEY idx_invoice_lines_invoice (invoice_id),

  CONSTRAINT fk_invoice_lines_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT chk_invoice_lines_qty CHECK (quantity > 0),
  CONSTRAINT chk_invoice_lines_amounts CHECK (unit_price >= 0 AND line_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT UNSIGNED NOT NULL,

  amount DECIMAL(10,2) NOT NULL,
  method ENUM('CARD','CASH','TRANSFER') NOT NULL DEFAULT 'CARD',
  status ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'SUCCESS',
  paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reference VARCHAR(80) NULL,

  PRIMARY KEY (id),
  KEY idx_payments_invoice (invoice_id),

  CONSTRAINT fk_payments_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT chk_payments_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================
-- STAFF OPS: shifts / attendance / payroll (hourly + overtime)
-- =========================
CREATE TABLE shifts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(60) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,

  PRIMARY KEY (id),
  UNIQUE KEY uk_shifts_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE staff_shift_assignments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  staff_user_id BIGINT UNSIGNED NOT NULL,
  shift_id BIGINT UNSIGNED NOT NULL,
  date_from DATE NOT NULL,
  date_to DATE NULL,

  PRIMARY KEY (id),
  KEY idx_staff_shift_staff (staff_user_id),
  KEY idx_staff_shift_shift (shift_id),

  CONSTRAINT fk_staff_shift_staff
    FOREIGN KEY (staff_user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT fk_staff_shift_shift
    FOREIGN KEY (shift_id) REFERENCES shifts(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Minutes-based attendance => accurate salary calculations
CREATE TABLE staff_attendance (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  staff_user_id BIGINT UNSIGNED NOT NULL,
  work_date DATE NOT NULL,

  check_in_at DATETIME NULL,
  check_out_at DATETIME NULL,

  regular_minutes INT NOT NULL DEFAULT 0,
  overtime_minutes INT NOT NULL DEFAULT 0,

  status ENUM('PRESENT','ABSENT','LATE','LEAVE') NOT NULL DEFAULT 'PRESENT',
  notes VARCHAR(255) NULL,

  PRIMARY KEY (id),
  UNIQUE KEY uk_attendance_unique (staff_user_id, work_date),
  KEY idx_attendance_date (work_date),

  CONSTRAINT fk_attendance_staff
    FOREIGN KEY (staff_user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT chk_attendance_minutes CHECK (regular_minutes >= 0 AND overtime_minutes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Payroll generated monthly based on attendance + hourly rates (store snapshot)
CREATE TABLE payroll (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  staff_user_id BIGINT UNSIGNED NOT NULL,
  period_year INT NOT NULL,
  period_month INT NOT NULL, -- 1..12

  regular_minutes INT NOT NULL DEFAULT 0,
  overtime_minutes INT NOT NULL DEFAULT 0,

  hourly_rate_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  overtime_hourly_rate_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0.00,

  regular_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  overtime_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00,

  bonus DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  deductions DECIMAL(10,2) NOT NULL DEFAULT 0.00,

  total_paid DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  paid_at DATETIME NULL,

  PRIMARY KEY (id),
  UNIQUE KEY uk_payroll_period (staff_user_id, period_year, period_month),

  CONSTRAINT fk_payroll_staff
    FOREIGN KEY (staff_user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT chk_payroll_month CHECK (period_month >= 1 AND period_month <= 12),

  CONSTRAINT chk_payroll_amounts CHECK (
    regular_minutes >= 0 AND overtime_minutes >= 0
    AND hourly_rate_snapshot >= 0 AND overtime_hourly_rate_snapshot >= 0
    AND regular_pay >= 0 AND overtime_pay >= 0
    AND bonus >= 0 AND deductions >= 0
    AND total_paid >= 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Views
CREATE VIEW annual_salary_by_employee AS
SELECT
  staff_user_id,
  period_year,
  SUM(total_paid) AS annual_total_paid
FROM payroll
GROUP BY staff_user_id, period_year;

CREATE VIEW annual_salary_with_employee AS
SELECT
  u.id AS staff_user_id,
  sp.first_name,
  sp.last_name,
  sp.position,
  p.period_year,
  SUM(p.total_paid) AS annual_total_paid
FROM payroll p
JOIN users u ON u.id = p.staff_user_id
JOIN staff_profiles sp ON sp.user_id = u.id
GROUP BY u.id, sp.first_name, sp.last_name, sp.position, p.period_year;

-- =========================
-- OPS: cleaning & maintenance requests
-- =========================
CREATE TABLE cleaning_requests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  room_id BIGINT UNSIGNED NOT NULL,
  reservation_id BIGINT UNSIGNED NULL,
  requested_by_user_id BIGINT UNSIGNED NULL,

  status ENUM('OPEN','IN_PROGRESS','DONE','CANCELLED') NOT NULL DEFAULT 'OPEN',
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  notes VARCHAR(255) NULL,

  PRIMARY KEY (id),
  KEY idx_cleaning_room (room_id),
  KEY idx_cleaning_status (status),

  CONSTRAINT fk_cleaning_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  CONSTRAINT fk_cleaning_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations(id)
    ON DELETE SET NULL ON UPDATE CASCADE,

  CONSTRAINT fk_cleaning_requested_by
    FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE maintenance_requests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  location_type ENUM('ROOM','FLOOR','OTHER') NOT NULL DEFAULT 'ROOM',
  room_id BIGINT UNSIGNED NULL,
  floor INT NULL,

  description TEXT NOT NULL,

  requested_by_user_id BIGINT UNSIGNED NULL,
  assigned_to_user_id BIGINT UNSIGNED NULL,

  status ENUM('OPEN','IN_PROGRESS','RESOLVED','CANCELLED') NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at DATETIME NULL,

  PRIMARY KEY (id),
  KEY idx_maintenance_status (status),
  KEY idx_maintenance_room (room_id),

  CONSTRAINT fk_maintenance_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE SET NULL ON UPDATE CASCADE,

  CONSTRAINT fk_maintenance_requested_by
    FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE,

  CONSTRAINT fk_maintenance_assigned_to
    FOREIGN KEY (assigned_to_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================
-- SECURITY: password reset + security questions
-- =========================
CREATE TABLE security_questions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_text VARCHAR(255) NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,

  PRIMARY KEY (id),
  UNIQUE KEY uk_security_questions_text (question_text)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_security_answers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  answer_hash VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_user_question (user_id, question_id),

  CONSTRAINT fk_user_sec_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT fk_user_sec_question
    FOREIGN KEY (question_id) REFERENCES security_questions(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_challenges (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  challenge_token VARCHAR(120) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_prc_token (challenge_token),
  KEY idx_prc_user (user_id),

  CONSTRAINT fk_prc_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_audit (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  event ENUM('REQUESTED','VERIFIED','RESET','FAILED') NOT NULL,
  ip_address VARCHAR(45) NULL,
  user_agent VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_pra_user (user_id),
  KEY idx_pra_created (created_at),

  CONSTRAINT fk_pra_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;