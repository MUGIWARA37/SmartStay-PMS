USE Smart_Hotel_v_1_0;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reservation_services;
DROP TABLE IF EXISTS invoice_lines;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS cleaning_requests;
DROP TABLE IF EXISTS maintenance_requests;
DROP TABLE IF EXISTS staff_shift_assignments;
DROP TABLE IF EXISTS staff_attendance;
DROP TABLE IF EXISTS shifts;
DROP TABLE IF EXISTS payroll;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS user_security_answers;
DROP TABLE IF EXISTS password_reset_audit;
DROP TABLE IF EXISTS password_reset_challenges;
DROP TABLE IF EXISTS security_questions;
DROP TABLE IF EXISTS room_images;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS staff_profiles;
DROP TABLE IF EXISTS guests;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS room_types;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(120) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('ADMIN','STAFF','CLIENT') NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE security_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_text VARCHAR(255) NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE user_security_answers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer_hash VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_usa_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_usa_question FOREIGN KEY (question_id) REFERENCES security_questions(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_challenges (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  challenge_status ENUM('OPEN','PASSED','FAILED','EXPIRED') NOT NULL DEFAULT 'OPEN',
  attempts INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  CONSTRAINT fk_prc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_prc_question FOREIGN KEY (question_id) REFERENCES security_questions(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  action_type VARCHAR(80) NOT NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  details JSON NULL,
  CONSTRAINT fk_pra_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE room_types (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(60) NOT NULL UNIQUE,
  description VARCHAR(255) NULL,
  price_per_night DECIMAL(10,2) NOT NULL,
  max_occupancy INT NOT NULL,
  amenities TEXT NULL
);

CREATE TABLE rooms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_number VARCHAR(10) NOT NULL UNIQUE,
  room_type_id BIGINT NOT NULL,
  floor INT NOT NULL,
  status ENUM('AVAILABLE','OCCUPIED','MAINTENANCE','CLEANING') NOT NULL DEFAULT 'AVAILABLE',
  notes VARCHAR(255) NULL,
  CONSTRAINT fk_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(id)
);

CREATE TABLE room_images (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  image_path VARCHAR(255) NOT NULL,
  is_primary TINYINT(1) NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 1,
  CONSTRAINT fk_room_images_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

CREATE TABLE guests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  email VARCHAR(120) NULL,
  phone VARCHAR(30) NULL,
  nationality VARCHAR(80) NULL,
  id_passport_number VARCHAR(50) NULL,
  preferences VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE staff_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  employee_code VARCHAR(40) NOT NULL UNIQUE,
  position VARCHAR(40) NOT NULL,
  department VARCHAR(80) NULL,
  hire_date DATE NOT NULL,
  salary_base DECIMAL(10,2) NOT NULL DEFAULT 0,
  emergency_contact VARCHAR(50) NULL,
  is_on_duty TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE reservations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guest_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  booked_by_user_id BIGINT NOT NULL,
  reservation_code VARCHAR(40) NOT NULL UNIQUE,
  check_in_date DATE NOT NULL,
  check_out_date DATE NOT NULL,
  adults_count INT NOT NULL DEFAULT 1,
  children_count INT NOT NULL DEFAULT 0,
  status ENUM('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'PENDING',
  special_requests VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_res_guest FOREIGN KEY (guest_id) REFERENCES guests(id),
  CONSTRAINT fk_res_room FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT fk_res_booked_by FOREIGN KEY (booked_by_user_id) REFERENCES users(id)
);

CREATE TABLE services (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE reservation_services (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rs_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
  CONSTRAINT fk_rs_service FOREIGN KEY (service_id) REFERENCES services(id)
);

CREATE TABLE invoices (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  invoice_number VARCHAR(50) NOT NULL UNIQUE,
  issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  subtotal_amount DECIMAL(10,2) NOT NULL,
  tax_amount DECIMAL(10,2) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status ENUM('DRAFT','ISSUED','PAID','CANCELLED') NOT NULL DEFAULT 'ISSUED',
  notes VARCHAR(255) NULL,
  CONSTRAINT fk_invoices_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
);

CREATE TABLE invoice_lines (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  invoice_id BIGINT NOT NULL,
  line_type ENUM('ROOM','SERVICE','OTHER') NOT NULL,
  reference_id BIGINT NULL,
  description VARCHAR(255) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL,
  line_total DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE TABLE payments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  invoice_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  method ENUM('CASH','CARD','TRANSFER') NOT NULL,
  paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  transaction_ref VARCHAR(100) NULL,
  status ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'SUCCESS',
  CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE TABLE shifts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shift_name VARCHAR(40) NOT NULL UNIQUE,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  is_night_shift TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE staff_shift_assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  staff_profile_id BIGINT NOT NULL,
  shift_id BIGINT NOT NULL,
  assigned_date DATE NOT NULL,
  assigned_by_user_id BIGINT NOT NULL,
  notes VARCHAR(255) NULL,
  CONSTRAINT fk_ssa_staff FOREIGN KEY (staff_profile_id) REFERENCES staff_profiles(id) ON DELETE CASCADE,
  CONSTRAINT fk_ssa_shift FOREIGN KEY (shift_id) REFERENCES shifts(id),
  CONSTRAINT fk_ssa_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id)
);

CREATE TABLE staff_attendance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  staff_profile_id BIGINT NOT NULL,
  date DATE NOT NULL,
  check_in DATETIME NULL,
  check_out DATETIME NULL,
  status ENUM('PRESENT','ABSENT','LATE','LEAVE') NOT NULL DEFAULT 'PRESENT',
  notes VARCHAR(255) NULL,
  CONSTRAINT fk_sa_staff FOREIGN KEY (staff_profile_id) REFERENCES staff_profiles(id) ON DELETE CASCADE
);

CREATE TABLE cleaning_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  requested_by_user_id BIGINT NOT NULL,
  assigned_to_staff_id BIGINT NULL,
  reservation_id BIGINT NULL,
  priority ENUM('LOW','MEDIUM','HIGH','URGENT') NOT NULL DEFAULT 'MEDIUM',
  status ENUM('NEW','ASSIGNED','IN_PROGRESS','DONE','CANCELLED') NOT NULL DEFAULT 'NEW',
  request_note VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  started_at DATETIME NULL,
  completed_at DATETIME NULL,
  CONSTRAINT fk_cr_room FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT fk_cr_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
  CONSTRAINT fk_cr_assigned_staff FOREIGN KEY (assigned_to_staff_id) REFERENCES staff_profiles(id),
  CONSTRAINT fk_cr_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE SET NULL
);

CREATE TABLE maintenance_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  reported_by_user_id BIGINT NOT NULL,
  assigned_to_staff_id BIGINT NULL,
  priority ENUM('LOW','MEDIUM','HIGH','URGENT') NOT NULL DEFAULT 'MEDIUM',
  status ENUM('NEW','ASSIGNED','IN_PROGRESS','RESOLVED','CANCELLED') NOT NULL DEFAULT 'NEW',
  title VARCHAR(120) NOT NULL,
  description VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at DATETIME NULL,
  CONSTRAINT fk_mr_room FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT fk_mr_reported_by FOREIGN KEY (reported_by_user_id) REFERENCES users(id),
  CONSTRAINT fk_mr_assigned_staff FOREIGN KEY (assigned_to_staff_id) REFERENCES staff_profiles(id)
);

CREATE TABLE payroll (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  staff_profile_id BIGINT NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  base_salary DECIMAL(10,2) NOT NULL,
  bonuses DECIMAL(10,2) NOT NULL DEFAULT 0,
  deductions DECIMAL(10,2) NOT NULL DEFAULT 0,
  net_salary DECIMAL(10,2) NOT NULL,
  generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at DATETIME NULL,
  status ENUM('GENERATED','PAID','CANCELLED') NOT NULL DEFAULT 'GENERATED',
  CONSTRAINT fk_payroll_staff FOREIGN KEY (staff_profile_id) REFERENCES staff_profiles(id) ON DELETE CASCADE
);