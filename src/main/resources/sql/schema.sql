-- =========================================
-- SmartStay PMS - FULL MySQL 8 Schema (NO DELIMITER/TRIGGERS)
-- Compatible with Spring SQL init/JDBC runners
-- =========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------
-- Drop views first
-- -----------------------------------------
DROP VIEW IF EXISTS annual_salary_with_employee;
DROP VIEW IF EXISTS annual_salary_by_employee;

-- -----------------------------------------
-- Drop tables (reverse dependency order)
-- -----------------------------------------
DROP TABLE IF EXISTS reservation_services;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS invoice_lines;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS guests;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS room_types;

DROP TABLE IF EXISTS password_reset_audit;
DROP TABLE IF EXISTS password_reset_challenges;
DROP TABLE IF EXISTS user_security_answers;
DROP TABLE IF EXISTS security_questions;

DROP TABLE IF EXISTS payroll;
DROP TABLE IF EXISTS staff_attendance;
DROP TABLE IF EXISTS staff_shift_assignments;
DROP TABLE IF EXISTS shifts;
DROP TABLE IF EXISTS staff_profiles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================
-- USERS & STAFF
-- =========================================

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('CLIENT','STAFF','ADMIN') NOT NULL,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  email VARCHAR(255),
  phone VARCHAR(30),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE staff_profiles (
  user_id BIGINT PRIMARY KEY,
  position VARCHAR(100),
  hire_date DATE,
  active TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_staff_profiles_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- SHIFTS / ATTENDANCE
-- =========================================

CREATE TABLE shifts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name ENUM('MORNING','MIDDAY','NIGHT') NOT NULL UNIQUE,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE staff_shift_assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  staff_id BIGINT NOT NULL,
  shift_id BIGINT NOT NULL,
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  CONSTRAINT fk_shift_assign_staff
    FOREIGN KEY (staff_id) REFERENCES staff_profiles(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_shift_assign_shift
    FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE staff_attendance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  staff_id BIGINT NOT NULL,
  shift_id BIGINT NOT NULL,
  shift_date DATE NOT NULL,
  check_in DATETIME NULL,
  check_out DATETIME NULL,
  was_late TINYINT(1) NOT NULL DEFAULT 0,
  notes TEXT NULL,
  CONSTRAINT fk_attendance_staff
    FOREIGN KEY (staff_id) REFERENCES staff_profiles(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_attendance_shift
    FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- MONTHLY PAYROLL (no triggers)
-- total_salary is generated automatically
-- =========================================

CREATE TABLE payroll (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  staff_id BIGINT NOT NULL,
  pay_month CHAR(7) NOT NULL, -- YYYY-MM
  regular_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  overtime_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  hourly_rate DECIMAL(10,2) NOT NULL,
  overtime_rate DECIMAL(10,2) NOT NULL,

  total_salary DECIMAL(12,2)
    GENERATED ALWAYS AS (
      (regular_hours * hourly_rate) + (overtime_hours * overtime_rate)
    ) STORED,

  status ENUM('DRAFT','APPROVED','PAID') NOT NULL DEFAULT 'DRAFT',
  paid_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT uq_payroll_staff_month UNIQUE (staff_id, pay_month),
  CONSTRAINT chk_payroll_hours CHECK (regular_hours >= 0 AND overtime_hours >= 0),
  CONSTRAINT chk_payroll_rates CHECK (hourly_rate >= 0 AND overtime_rate >= 0),
  CONSTRAINT chk_pay_month_format CHECK (pay_month REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$'),
  CONSTRAINT fk_payroll_staff
    FOREIGN KEY (staff_id) REFERENCES staff_profiles(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payroll_staff_month ON payroll(staff_id, pay_month);
CREATE INDEX idx_payroll_month ON payroll(pay_month);

-- =========================================
-- ROOMS / GUESTS / RESERVATIONS
-- =========================================

CREATE TABLE room_types (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE,
  base_rate DECIMAL(10,2) NOT NULL,
  max_occupancy INT NOT NULL,
  description TEXT NULL,
  CONSTRAINT chk_room_types_rate CHECK (base_rate >= 0),
  CONSTRAINT chk_room_types_occ CHECK (max_occupancy > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rooms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_number VARCHAR(20) NOT NULL UNIQUE,
  room_type_id BIGINT NOT NULL,
  status ENUM('AVAILABLE','OCCUPIED','OUT_OF_SERVICE') NOT NULL,
  floor INT NULL,
  notes TEXT NULL,
  CONSTRAINT fk_rooms_type
    FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE guests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  email VARCHAR(255),
  phone VARCHAR(30),
  doc_id VARCHAR(100),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_guests_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reservations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guest_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  check_in_date DATE NOT NULL,
  check_out_date DATE NOT NULL,
  status ENUM('BOOKED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW') NOT NULL,
  adults INT NOT NULL DEFAULT 1,
  children INT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_res_dates CHECK (check_out_date > check_in_date),
  CONSTRAINT chk_res_adults CHECK (adults >= 1),
  CONSTRAINT chk_res_children CHECK (children >= 0),
  CONSTRAINT fk_res_guest
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE RESTRICT,
  CONSTRAINT fk_res_room
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_res_room_dates ON reservations(room_id, check_in_date, check_out_date);

-- =========================================
-- BILLING
-- =========================================

CREATE TABLE invoices (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  guest_id BIGINT NOT NULL,
  issue_date DATE NOT NULL,
  due_date DATE NULL,
  total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  status ENUM('DRAFT','ISSUED','PAID','VOID') NOT NULL,
  CONSTRAINT chk_invoices_total CHECK (total_amount >= 0),
  CONSTRAINT fk_invoices_res
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE RESTRICT,
  CONSTRAINT fk_invoices_guest
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_lines (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  invoice_id BIGINT NOT NULL,
  description VARCHAR(255) NOT NULL,
  quantity DECIMAL(10,2) NOT NULL DEFAULT 1.00,
  unit_price DECIMAL(10,2) NOT NULL,
  line_total DECIMAL(10,2) NOT NULL,
  CONSTRAINT chk_invoice_lines_qty CHECK (quantity > 0),
  CONSTRAINT chk_invoice_lines_prices CHECK (unit_price >= 0 AND line_total >= 0),
  CONSTRAINT fk_invoice_lines_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  invoice_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  method ENUM('CASH','CARD','ONLINE','BANK_TRANSFER') NOT NULL,
  paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reference VARCHAR(255),
  CONSTRAINT chk_payments_amount CHECK (amount > 0),
  CONSTRAINT fk_payments_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- SERVICES / EXTRAS
-- =========================================

CREATE TABLE services (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE,
  default_price DECIMAL(10,2) NOT NULL,
  type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
  CONSTRAINT chk_services_price CHECK (default_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reservation_services (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  quantity DECIMAL(10,2) NOT NULL DEFAULT 1.00,
  unit_price DECIMAL(10,2) NOT NULL,
  line_total DECIMAL(10,2) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_res_services_qty CHECK (quantity > 0),
  CONSTRAINT chk_res_services_prices CHECK (unit_price >= 0 AND line_total >= 0),
  CONSTRAINT fk_res_services_res
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
  CONSTRAINT fk_res_services_service
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================
-- PASSWORD RECOVERY TABLES
-- =========================================

CREATE TABLE security_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_code VARCHAR(10) NOT NULL UNIQUE,
  question_text VARCHAR(255) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_security_answers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer_hash VARCHAR(255) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_user_question UNIQUE (user_id, question_id),
  CONSTRAINT fk_user_sec_answers_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_sec_answers_question
    FOREIGN KEY (question_id) REFERENCES security_questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_sec_answers_user ON user_security_answers(user_id);

CREATE TABLE password_reset_challenges (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  question1_id BIGINT NOT NULL,
  question2_id BIGINT NOT NULL,
  question3_id BIGINT NOT NULL,
  attempts_used INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 3,
  verified TINYINT(1) NOT NULL DEFAULT 0,
  expires_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_reset_attempts CHECK (attempts_used >= 0 AND max_attempts > 0),
  CONSTRAINT chk_reset_questions_distinct CHECK (
    question1_id <> question2_id AND
    question1_id <> question3_id AND
    question2_id <> question3_id
  ),
  CONSTRAINT fk_reset_challenges_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_reset_challenges_q1
    FOREIGN KEY (question1_id) REFERENCES security_questions(id) ON DELETE RESTRICT,
  CONSTRAINT fk_reset_challenges_q2
    FOREIGN KEY (question2_id) REFERENCES security_questions(id) ON DELETE RESTRICT,
  CONSTRAINT fk_reset_challenges_q3
    FOREIGN KEY (question3_id) REFERENCES security_questions(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_reset_challenges_user ON password_reset_challenges(user_id);
CREATE INDEX idx_reset_challenges_expires ON password_reset_challenges(expires_at);

CREATE TABLE password_reset_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  challenge_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  success TINYINT(1) NOT NULL,
  ip_address VARCHAR(64),
  user_agent VARCHAR(512),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reset_audit_challenge
    FOREIGN KEY (challenge_id) REFERENCES password_reset_challenges(id) ON DELETE CASCADE,
  CONSTRAINT fk_reset_audit_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_reset_audit_user_time ON password_reset_audit(user_id, created_at);

-- =========================================
-- VIEWS (annual salary)
-- =========================================

CREATE VIEW annual_salary_by_employee AS
SELECT
  p.staff_id,
  LEFT(p.pay_month, 4) AS year,
  ROUND(SUM(p.total_salary), 2) AS annual_salary
FROM payroll p
GROUP BY p.staff_id, LEFT(p.pay_month, 4);

CREATE VIEW annual_salary_with_employee AS
SELECT
  p.staff_id,
  u.username,
  u.first_name,
  u.last_name,
  LEFT(p.pay_month, 4) AS year,
  ROUND(SUM(p.total_salary), 2) AS annual_salary
FROM payroll p
JOIN users u ON u.id = p.staff_id
GROUP BY p.staff_id, u.username, u.first_name, u.last_name, LEFT(p.pay_month, 4);