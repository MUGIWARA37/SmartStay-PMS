-- =========================================
-- SmartStay PMS - MySQL 8 seed.sql
-- Compatible with the provided schema.sql
-- Time-dynamic data using CURDATE()/NOW()
-- =========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Optional cleanup (safe order)
DELETE FROM password_reset_audit;
DELETE FROM password_reset_challenges;
DELETE FROM user_security_answers;
DELETE FROM security_questions;

DELETE FROM reservation_services;
DELETE FROM services;
DELETE FROM payments;
DELETE FROM invoice_lines;
DELETE FROM invoices;
DELETE FROM reservations;
DELETE FROM guests;
DELETE FROM rooms;
DELETE FROM room_types;

DELETE FROM payroll;
DELETE FROM staff_attendance;
DELETE FROM staff_shift_assignments;
DELETE FROM shifts;
DELETE FROM staff_profiles;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================
-- USERS (staff + admin + sample clients)
-- =========================================
INSERT INTO users (id, username, password_hash, role, first_name, last_name, email, phone) VALUES
(1, 'admin', 'HASH_ADMIN_BCRYPT', 'ADMIN', 'System', 'Admin', 'admin@smartstay.com', '+1-555-0001'),

(101, 'carlos.mendes',  'HASH_STAFF_BCRYPT', 'STAFF', 'Carlos',  'Mendes',   'carlos.mendes@smartstay.com',  '+1-555-0301'),
(102, 'huda.alami',     'HASH_STAFF_BCRYPT', 'STAFF', 'Huda',    'Alami',    'huda.alami@smartstay.com',     '+1-555-0302'),
(103, 'ethan.brooks',   'HASH_STAFF_BCRYPT', 'STAFF', 'Ethan',   'Brooks',   'ethan.brooks@smartstay.com',   '+1-555-0303'),
(104, 'mireille.ndour', 'HASH_STAFF_BCRYPT', 'STAFF', 'Mireille','N''Dour',  'mireille.ndour@smartstay.com', '+1-555-0304'),
(105, 'rafael.costa',   'HASH_STAFF_BCRYPT', 'STAFF', 'Rafael',  'Costa',    'rafael.costa@smartstay.com',   '+1-555-0305'),

(201, 'amira.hassan',   'HASH_CLIENT_BCRYPT', 'CLIENT', 'Amira',  'Hassan',   'amira.hassan@email.com', '+1-555-0192'),
(202, 'luca.bianchi',   'HASH_CLIENT_BCRYPT', 'CLIENT', 'Luca',   'Bianchi',  'luca.bianchi@email.com', '+1-555-0193'),
(203, 'aiko.tanaka',    'HASH_CLIENT_BCRYPT', 'CLIENT', 'Aiko',   'Tanaka',   'aiko.tanaka@email.com',  '+1-555-0194'),
(204, 'kwame.mensah',   'HASH_CLIENT_BCRYPT', 'CLIENT', 'Kwame',  'Mensah',   'kwame.mensah@email.com', '+1-555-0195'),
(205, 'sofia.rojas',    'HASH_CLIENT_BCRYPT', 'CLIENT', 'Sofia',  'Rojas',    'sofia.rojas@email.com',  '+1-555-0196');

-- =========================================
-- STAFF PROFILES
-- =========================================
INSERT INTO staff_profiles (user_id, position, hire_date, active) VALUES
(101, 'manager',      DATE_SUB(CURDATE(), INTERVAL 4 YEAR), 1),
(102, 'receptionist', DATE_SUB(CURDATE(), INTERVAL 3 YEAR), 1),
(103, 'receptionist', DATE_SUB(CURDATE(), INTERVAL 5 YEAR), 1),
(104, 'concierge',    DATE_SUB(CURDATE(), INTERVAL 6 YEAR), 1),
(105, 'security',     DATE_SUB(CURDATE(), INTERVAL 2 YEAR), 1);

-- =========================================
-- SHIFTS
-- =========================================
INSERT INTO shifts (id, name, start_time, end_time) VALUES
(1, 'MORNING', '07:00:00', '15:00:00'),
(2, 'MIDDAY',  '15:00:00', '23:00:00'),
(3, 'NIGHT',   '23:00:00', '07:00:00');

-- staff shift assignments (active window)
INSERT INTO staff_shift_assignments (staff_id, shift_id, effective_from, effective_to) VALUES
(101, 1, DATE_SUB(CURDATE(), INTERVAL 60 DAY), NULL),
(102, 1, DATE_SUB(CURDATE(), INTERVAL 60 DAY), NULL),
(103, 2, DATE_SUB(CURDATE(), INTERVAL 60 DAY), NULL),
(104, 2, DATE_SUB(CURDATE(), INTERVAL 60 DAY), NULL),
(105, 3, DATE_SUB(CURDATE(), INTERVAL 60 DAY), NULL);

-- =========================================
-- ROOM TYPES
-- =========================================
INSERT INTO room_types (id, name, base_rate, max_occupancy, description) VALUES
(1, 'Standard Single', 165.00, 1, 'Standard single room'),
(2, 'Deluxe Double',   245.00, 2, 'Deluxe double room'),
(3, 'Junior Suite',    370.00, 3, 'Junior suite'),
(4, 'Executive Suite', 575.00, 4, 'Executive suite'),
(5, 'Penthouse',      1225.00, 6, 'Luxury penthouse'),
(6, 'Accessible Room', 195.00, 2, 'Accessible guest room');

-- =========================================
-- ROOMS (sample set)
-- status enum: AVAILABLE, OCCUPIED, OUT_OF_SERVICE
-- =========================================
INSERT INTO rooms (id, room_number, room_type_id, status, floor, notes) VALUES
(101, '101', 6, 'OCCUPIED', 1, 'Lobby Wing | accessible'),
(102, '102', 1, 'AVAILABLE', 1, 'Lobby Wing'),
(103, '103', 1, 'AVAILABLE', 1, 'Lobby Wing'),
(104, '104', 1, 'OUT_OF_SERVICE', 1, 'Lobby Wing | HVAC maintenance'),

(201, '201', 1, 'OCCUPIED', 2, 'Garden Wing'),
(202, '202', 2, 'OCCUPIED', 2, 'Garden Wing'),
(203, '203', 1, 'AVAILABLE', 2, 'Garden Wing'),

(301, '301', 2, 'OCCUPIED', 3, 'Executive Wing'),
(302, '302', 2, 'AVAILABLE', 3, 'Executive Wing'),
(303, '303', 3, 'OCCUPIED', 3, 'Executive Wing'),

(401, '401', 3, 'OCCUPIED', 4, 'Premium Wing'),
(402, '402', 4, 'OCCUPIED', 4, 'Premium Wing'),
(403, '403', 3, 'AVAILABLE', 4, 'Premium Wing'),

(501, '501', 4, 'OCCUPIED', 5, 'Penthouse Wing'),
(515, '515', 5, 'OCCUPIED', 5, 'Penthouse Wing');

-- =========================================
-- GUESTS
-- =========================================
INSERT INTO guests (id, user_id, first_name, last_name, email, phone, doc_id) VALUES
(1, 201, 'Amira', 'Hassan', 'amira.hassan@email.com', '+1-555-0192', 'G-0001|A12345678'),
(2, 202, 'Luca', 'Bianchi', 'luca.bianchi@email.com', '+1-555-0193', 'G-0002|YA7788211'),
(3, 203, 'Aiko', 'Tanaka', 'aiko.tanaka@email.com', '+1-555-0194', 'G-0003|TK3399172'),
(4, 204, 'Kwame', 'Mensah', 'kwame.mensah@email.com', '+1-555-0195', 'G-0004|GH9981230'),
(5, 205, 'Sofia', 'Rojas', 'sofia.rojas@email.com', '+1-555-0196', 'G-0005|CL6612045');

-- =========================================
-- RESERVATIONS (dynamic dates)
-- status enum: BOOKED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW
-- =========================================
INSERT INTO reservations
(id, guest_id, room_id, check_in_date, check_out_date, status, adults, children, created_at)
VALUES
-- active / checked-in
(9001, 1, 303, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'CHECKED_IN', 2, 1, NOW()),
(9002, 2, 202, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'CHECKED_IN', 2, 0, NOW()),
(9003, 3, 401, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'CHECKED_IN', 1, 0, NOW()),

-- past / checked-out
(9004, 4, 201, DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_SUB(CURDATE(), INTERVAL 12 DAY), 'CHECKED_OUT', 1, 0, NOW()),
(9005, 5, 515, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 17 DAY), 'CHECKED_OUT', 2, 0, NOW()),

-- future / booked
(9006, 1, 302, DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY), 'BOOKED', 1, 0, NOW()),
(9007, 2, 403, DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 'BOOKED', 2, 0, NOW()),

-- cancelled / no_show
(9008, 3, 102, DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'CANCELLED', 1, 0, NOW()),
(9009, 4, 103, DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'NO_SHOW', 1, 0, NOW());

-- =========================================
-- INVOICES
-- =========================================
INSERT INTO invoices
(id, reservation_id, guest_id, issue_date, due_date, total_amount, currency, status)
VALUES
(7001, 9001, 1, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 4 DAY), 1620.00, 'USD', 'ISSUED'),
(7002, 9002, 2, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 DAY), 864.00, 'USD', 'PAID'),
(7003, 9003, 3, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1260.00, 'USD', 'ISSUED'),
(7004, 9004, 4, DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY), 555.00, 'USD', 'PAID'),
(7005, 9005, 5, DATE_SUB(CURDATE(), INTERVAL 17 DAY), DATE_SUB(CURDATE(), INTERVAL 15 DAY), 3420.00, 'USD', 'PAID');

INSERT INTO invoice_lines (invoice_id, description, quantity, unit_price, line_total) VALUES
(7001, 'Room charge', 5, 360.00, 1800.00),
(7001, 'Discount',    1, -180.00, -180.00),

(7002, 'Room charge', 4, 240.00, 960.00),
(7002, 'Discount',    1, -96.00, -96.00),

(7003, 'Room charge', 3, 420.00, 1260.00),
(7004, 'Room charge', 3, 185.00, 555.00),
(7005, 'Room charge', 3, 1200.00, 3600.00),
(7005, 'Discount',    1, -180.00, -180.00);

INSERT INTO payments (invoice_id, amount, method, paid_at, reference) VALUES
(7002, 864.00,  'BANK_TRANSFER', NOW(), 'BK-DYN-9002'),
(7004, 555.00,  'CASH',          DATE_SUB(NOW(), INTERVAL 11 DAY), 'BK-DYN-9004'),
(7005, 3420.00, 'CARD',          DATE_SUB(NOW(), INTERVAL 16 DAY), 'BK-DYN-9005');

-- =========================================
-- SERVICES / RESERVATION SERVICES
-- =========================================
INSERT INTO services (id, name, default_price, type) VALUES
(1, 'Airport Transfer', 80.00, 'TRANSPORT'),
(2, 'Spa Session',      120.00, 'WELLNESS'),
(3, 'Laundry',          25.00, 'HOUSEKEEPING'),
(4, 'Extra Bed',        40.00, 'ROOM');

INSERT INTO reservation_services (reservation_id, service_id, quantity, unit_price, line_total, created_at) VALUES
(9001, 1, 1, 80.00, 80.00, NOW()),
(9001, 2, 2, 120.00, 240.00, NOW()),
(9002, 3, 3, 25.00, 75.00, NOW()),
(9003, 4, 1, 40.00, 40.00, NOW());

-- =========================================
-- PAYROLL (dynamic months)
-- pay_month must be YYYY-MM
-- total_salary auto-generated by schema
-- =========================================
INSERT INTO payroll
(staff_id, pay_month, regular_hours, overtime_hours, hourly_rate, overtime_rate, status, paid_at)
VALUES
(101, DATE_FORMAT(CURDATE(), '%Y-%m'), 176.00, 12.00, 35.00, 52.50, 'APPROVED', NULL),
(102, DATE_FORMAT(CURDATE(), '%Y-%m'), 168.00, 10.00, 20.00, 30.00, 'PAID', NOW()),
(103, DATE_FORMAT(CURDATE(), '%Y-%m'), 168.00,  8.00, 20.00, 30.00, 'PAID', NOW()),
(104, DATE_FORMAT(CURDATE(), '%Y-%m'), 170.00,  6.00, 24.00, 36.00, 'APPROVED', NULL),
(105, DATE_FORMAT(CURDATE(), '%Y-%m'), 180.00, 14.00, 18.00, 27.00, 'DRAFT', NULL),

(101, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 172.00, 9.00, 35.00, 52.50, 'PAID', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(102, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 168.00, 6.00, 20.00, 30.00, 'PAID', DATE_SUB(NOW(), INTERVAL 20 DAY));

-- =========================================
-- SECURITY QUESTIONS (10 fixed)
-- =========================================
INSERT INTO security_questions (id, question_code, question_text, active) VALUES
(1,  'Q01', 'What was the name of your first pet?', 1),
(2,  'Q02', 'In what city were you born?', 1),
(3,  'Q03', 'What is your mother''s maiden name?', 1),
(4,  'Q04', 'What was the model of your first car?', 1),
(5,  'Q05', 'What was the name of your elementary school?', 1),
(6,  'Q06', 'What is your favorite book?', 1),
(7,  'Q07', 'What street did you grow up on?', 1),
(8,  'Q08', 'What is the name of your childhood best friend?', 1),
(9,  'Q09', 'What was your first job title?', 1),
(10, 'Q10', 'What is your favorite teacher''s last name?', 1);

-- users store answers for all 10 questions (hash placeholders)
INSERT INTO user_security_answers (user_id, question_id, answer_hash) VALUES
(201,1,'HASH_A1'),(201,2,'HASH_A2'),(201,3,'HASH_A3'),(201,4,'HASH_A4'),(201,5,'HASH_A5'),
(201,6,'HASH_A6'),(201,7,'HASH_A7'),(201,8,'HASH_A8'),(201,9,'HASH_A9'),(201,10,'HASH_A10'),

(202,1,'HASH_B1'),(202,2,'HASH_B2'),(202,3,'HASH_B3'),(202,4,'HASH_B4'),(202,5,'HASH_B5'),
(202,6,'HASH_B6'),(202,7,'HASH_B7'),(202,8,'HASH_B8'),(202,9,'HASH_B9'),(202,10,'HASH_B10');

-- sample active challenge for testing (3 out of 10)
INSERT INTO password_reset_challenges
(user_id, token, question1_id, question2_id, question3_id, attempts_used, max_attempts, verified, expires_at, created_at)
VALUES
(201, CONCAT('PRC-', UNIX_TIMESTAMP()), 2, 5, 9, 0, 3, 0, DATE_ADD(NOW(), INTERVAL 15 MINUTE), NOW());

INSERT INTO password_reset_audit (challenge_id, user_id, success, ip_address, user_agent, created_at)
SELECT id, user_id, 0, '127.0.0.1', 'seed-script', NOW()
FROM password_reset_challenges
ORDER BY id DESC
LIMIT 1;