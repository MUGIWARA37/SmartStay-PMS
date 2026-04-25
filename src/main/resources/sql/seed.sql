-- SmartStay PMS - Seed data (dynamic dates based on NOW()/CURDATE())
-- Works with schema.sql provided previously

SET NAMES utf8mb4;
SET time_zone = '+00:00';

START TRANSACTION;

-- =========================
-- USERS
-- =========================
-- Passwords here are demo strings (NOT secure). Later you should store BCrypt hashes.
INSERT INTO users (username, email, password_hash, role, is_active, last_login_at, created_at)
VALUES
  ('admin', 'admin@smartstay.local', 'admin123', 'ADMIN', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
  ('reception1', 'reception1@smartstay.local', 'staff123', 'STAFF', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),
  ('cleaning1', 'cleaning1@smartstay.local', 'staff123', 'STAFF', 1, NULL, DATE_SUB(NOW(), INTERVAL 18 DAY)),
  ('maint1', 'maint1@smartstay.local', 'staff123', 'STAFF', 1, NULL, DATE_SUB(NOW(), INTERVAL 18 DAY)),
  ('client1', 'client1@mail.com', 'client123', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 10 DAY)),
  ('client2', 'client2@mail.com', 'client123', 'CLIENT', 1, NULL, DATE_SUB(NOW(), INTERVAL 12 DAY));

-- STAFF PROFILES (hourly + overtime)
INSERT INTO staff_profiles (user_id, first_name, last_name, phone, cin, position, hourly_rate, overtime_hourly_rate, hired_at)
SELECT id, 'Hassan', 'Admin', '0600000000', 'AA000001', 'reception', 0.00, 0.00, DATE_SUB(CURDATE(), INTERVAL 365 DAY)
FROM users WHERE username='admin';

INSERT INTO staff_profiles (user_id, first_name, last_name, phone, cin, position, hourly_rate, overtime_hourly_rate, hired_at)
SELECT id, 'Sara', 'Reception', '0600000001', 'BB000002', 'reception', 35.00, 55.00, DATE_SUB(CURDATE(), INTERVAL 120 DAY)
FROM users WHERE username='reception1';

INSERT INTO staff_profiles (user_id, first_name, last_name, phone, cin, position, hourly_rate, overtime_hourly_rate, hired_at)
SELECT id, 'Youssef', 'Cleaning', '0600000002', 'CC000003', 'cleaning', 25.00, 40.00, DATE_SUB(CURDATE(), INTERVAL 200 DAY)
FROM users WHERE username='cleaning1';

INSERT INTO staff_profiles (user_id, first_name, last_name, phone, cin, position, hourly_rate, overtime_hourly_rate, hired_at)
SELECT id, 'Khadija', 'Maintenance', '0600000003', 'DD000004', 'maintenance', 30.00, 50.00, DATE_SUB(CURDATE(), INTERVAL 150 DAY)
FROM users WHERE username='maint1';

-- =========================
-- ROOM TYPES
-- =========================
INSERT INTO room_types (name, description, base_price_per_night, capacity_adults, capacity_children, bed_type, is_active)
VALUES
  ('Standard', 'Cozy standard room with samurai-inspired decor.', 450.00, 2, 0, 'Queen', 1),
  ('Deluxe', 'Larger room with premium view and gold accents.', 650.00, 2, 1, 'King', 1),
  ('Suite', 'Spacious suite with lounge area and luxury amenities.', 950.00, 3, 2, 'King', 1);

-- =========================
-- ROOMS
-- =========================
INSERT INTO rooms (room_number, floor, room_type_id, status, notes)
VALUES
  ('101', 1, (SELECT id FROM room_types WHERE name='Standard'), 'AVAILABLE', NULL),
  ('102', 1, (SELECT id FROM room_types WHERE name='Standard'), 'DIRTY', 'Needs deep cleaning after checkout'),
  ('201', 2, (SELECT id FROM room_types WHERE name='Deluxe'), 'AVAILABLE', NULL),
  ('202', 2, (SELECT id FROM room_types WHERE name='Deluxe'), 'MAINTENANCE', 'AC issue reported'),
  ('301', 3, (SELECT id FROM room_types WHERE name='Suite'), 'OCCUPIED', 'VIP guest');

-- =========================
-- ROOM IMAGES (multiple images per room)
-- Note: image_path are example resource paths. Put files under src/main/resources/images/...
-- =========================
-- Room 101
INSERT INTO room_images (room_id, image_path, alt_text, sort_order, is_primary)
VALUES
  ((SELECT id FROM rooms WHERE room_number='101'), '/images/rooms/101/1.jpg', 'Room 101 - main', 1, 1),
  ((SELECT id FROM rooms WHERE room_number='101'), '/images/rooms/101/2.jpg', 'Room 101 - angle', 2, 0),
  ((SELECT id FROM rooms WHERE room_number='101'), '/images/rooms/101/3.jpg', 'Room 101 - bathroom', 3, 0);

-- Room 201
INSERT INTO room_images (room_id, image_path, alt_text, sort_order, is_primary)
VALUES
  ((SELECT id FROM rooms WHERE room_number='201'), '/images/rooms/201/1.jpg', 'Room 201 - main', 1, 1),
  ((SELECT id FROM rooms WHERE room_number='201'), '/images/rooms/201/2.jpg', 'Room 201 - view', 2, 0);

-- Room 301
INSERT INTO room_images (room_id, image_path, alt_text, sort_order, is_primary)
VALUES
  ((SELECT id FROM rooms WHERE room_number='301'), '/images/rooms/301/1.jpg', 'Room 301 - main', 1, 1),
  ((SELECT id FROM rooms WHERE room_number='301'), '/images/rooms/301/2.jpg', 'Room 301 - lounge', 2, 0),
  ((SELECT id FROM rooms WHERE room_number='301'), '/images/rooms/301/3.jpg', 'Room 301 - details', 3, 0);

-- =========================
-- SERVICES
-- =========================
INSERT INTO services (name, description, price, is_active)
VALUES
  ('Breakfast', 'Traditional breakfast served 7:00-10:30', 60.00, 1),
  ('Airport Pickup', 'One-way pickup from airport', 200.00, 1),
  ('Spa Access', 'Access to spa for one day', 150.00, 1);

-- =========================
-- GUESTS
-- Link guest#1 to client1 user for demo
-- =========================
INSERT INTO guests (user_id, first_name, last_name, email, phone, cin, address, created_at)
VALUES
  ((SELECT id FROM users WHERE username='client1'), 'Omar', 'El Amrani', 'client1@mail.com', '0611111111', 'GUEST001', 'Khouribga, Morocco', DATE_SUB(NOW(), INTERVAL 9 DAY)),
  (NULL, 'Imane', 'Zouhair', 'imane@mail.com', '0622222222', 'GUEST002', 'Casablanca, Morocco', DATE_SUB(NOW(), INTERVAL 11 DAY));

-- =========================
-- RESERVATIONS (dynamic dates)
-- Create one current reservation (checked in) and one upcoming confirmed
-- =========================
-- Current: started yesterday, ends tomorrow
INSERT INTO reservations (
  reservation_code, guest_id, room_id,
  check_in_date, check_out_date,
  status, adults, children, created_by_user_id,
  created_at, updated_at
)
VALUES (
  CONCAT('RES-', DATE_FORMAT(NOW(), '%Y%m%d'), '-A'),
  (SELECT id FROM guests WHERE cin='GUEST001'),
  (SELECT id FROM rooms WHERE room_number='301'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY),
  DATE_ADD(CURDATE(), INTERVAL 1 DAY),
  'CHECKED_IN',
  2, 0,
  (SELECT id FROM users WHERE username='reception1'),
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  NOW()
);

-- Upcoming: starts in 3 days, ends in 6 days
INSERT INTO reservations (
  reservation_code, guest_id, room_id,
  check_in_date, check_out_date,
  status, adults, children, created_by_user_id,
  created_at, updated_at
)
VALUES (
  CONCAT('RES-', DATE_FORMAT(NOW(), '%Y%m%d'), '-B'),
  (SELECT id FROM guests WHERE cin='GUEST002'),
  (SELECT id FROM rooms WHERE room_number='201'),
  DATE_ADD(CURDATE(), INTERVAL 3 DAY),
  DATE_ADD(CURDATE(), INTERVAL 6 DAY),
  'CONFIRMED',
  2, 1,
  (SELECT id FROM users WHERE username='reception1'),
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  NOW()
);

-- Add services to the current reservation
INSERT INTO reservation_services (reservation_id, service_id, quantity, unit_price, created_at)
VALUES
  ((SELECT id FROM reservations WHERE reservation_code LIKE CONCAT('RES-', DATE_FORMAT(NOW(), '%Y%m%d'), '-A')),
   (SELECT id FROM services WHERE name='Breakfast'),
   2, (SELECT price FROM services WHERE name='Breakfast'),
   NOW()),
  ((SELECT id FROM reservations WHERE reservation_code LIKE CONCAT('RES-', DATE_FORMAT(NOW(), '%Y%m%d'), '-A')),
   (SELECT id FROM services WHERE name='Spa Access'),
   1, (SELECT price FROM services WHERE name='Spa Access'),
   NOW());

-- =========================
-- INVOICE + LINES + PAYMENT (for current reservation)
-- =========================
INSERT INTO invoices (invoice_number, reservation_id, issued_at, status, currency, subtotal, tax, discount, total)
VALUES (
  CONCAT('INV-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001'),
  (SELECT id FROM reservations WHERE reservation_code LIKE CONCAT('RES-', DATE_FORMAT(NOW(), '%Y%m%d'), '-A')),
  NOW(),
  'ISSUED',
  'MAD',
  0.00, 0.00, 0.00, 0.00
);

-- Room nights line (2 nights: yesterday->today, today->tomorrow)
INSERT INTO invoice_lines (invoice_id, line_type, description, quantity, unit_price, line_total)
VALUES (
  (SELECT id FROM invoices WHERE invoice_number = CONCAT('INV-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001')),
  'ROOM_NIGHT',
  'Suite - Room nights',
  2,
  (SELECT rt.base_price_per_night
     FROM reservations r
     JOIN rooms rm ON rm.id = r.room_id
     JOIN room_types rt ON rt.id = rm.room_type_id
    WHERE r.reservation_code LIKE CONCAT('RES-', DATE_FORMAT(NOW(), '%Y%m%d'), '-A')),
  2 * (SELECT rt.base_price_per_night
         FROM reservations r
         JOIN rooms rm ON rm.id = r.room_id
         JOIN room_types rt ON rt.id = rm.room_type_id
        WHERE r.reservation_code LIKE CONCAT('RES-', DATE_FORMAT(NOW(), '%Y%m%d'), '-A'))
);

-- Services lines (snapshot)
INSERT INTO invoice_lines (invoice_id, line_type, description, quantity, unit_price, line_total)
SELECT
  (SELECT id FROM invoices WHERE invoice_number = CONCAT('INV-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001')) AS invoice_id,
  'SERVICE',
  s.name,
  rs.quantity,
  rs.unit_price,
  rs.quantity * rs.unit_price
FROM reservation_services rs
JOIN services s ON s.id = rs.service_id
WHERE rs.reservation_id = (SELECT id FROM reservations WHERE reservation_code LIKE CONCAT('RES-', DATE_FORMAT(NOW(), '%Y%m%d'), '-A'));

-- Update invoice totals (subtotal = sum lines; tax/discount=0 for demo)
UPDATE invoices i
SET
  i.subtotal = (SELECT COALESCE(SUM(il.line_total),0) FROM invoice_lines il WHERE il.invoice_id = i.id),
  i.tax = 0.00,
  i.discount = 0.00,
  i.total = (SELECT COALESCE(SUM(il.line_total),0) FROM invoice_lines il WHERE il.invoice_id = i.id)
WHERE i.invoice_number = CONCAT('INV-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001');

-- Partial payment (dynamic amount: half the total)
INSERT INTO payments (invoice_id, amount, method, status, paid_at, reference)
VALUES (
  (SELECT id FROM invoices WHERE invoice_number = CONCAT('INV-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001')),
  (SELECT ROUND(total / 2, 2) FROM invoices WHERE invoice_number = CONCAT('INV-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001')),
  'CARD',
  'SUCCESS',
  NOW(),
  CONCAT('PAY-', DATE_FORMAT(NOW(), '%Y%m%d%H%i%S'))
);

-- =========================
-- SHIFTS + ASSIGNMENTS
-- =========================
INSERT INTO shifts (name, start_time, end_time, is_active)
VALUES
  ('Morning', '08:00:00', '16:00:00', 1),
  ('Evening', '16:00:00', '00:00:00', 1);

-- Assign reception1 to Morning shift starting 30 days ago
INSERT INTO staff_shift_assignments (staff_user_id, shift_id, date_from, date_to)
VALUES (
  (SELECT id FROM users WHERE username='reception1'),
  (SELECT id FROM shifts WHERE name='Morning'),
  DATE_SUB(CURDATE(), INTERVAL 30 DAY),
  NULL
);

-- =========================
-- ATTENDANCE (last 5 days) + PAYROLL (this month)
-- =========================
-- Create attendance rows for reception1: 8h regular (480min) + 1h overtime (60min) for the last 5 days
INSERT INTO staff_attendance (staff_user_id, work_date, check_in_at, check_out_at, regular_minutes, overtime_minutes, status, notes)
SELECT
  (SELECT id FROM users WHERE username='reception1') AS staff_user_id,
  DATE_SUB(CURDATE(), INTERVAL d.n DAY) AS work_date,
  DATE_SUB(CONCAT(DATE_SUB(CURDATE(), INTERVAL d.n DAY), ' 08:00:00'), INTERVAL 0 MINUTE) AS check_in_at,
  DATE_SUB(CONCAT(DATE_SUB(CURDATE(), INTERVAL d.n DAY), ' 17:00:00'), INTERVAL 0 MINUTE) AS check_out_at,
  480 AS regular_minutes,
  60 AS overtime_minutes,
  'PRESENT' AS status,
  'Auto-seeded attendance' AS notes
FROM (
  SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) d;

-- Payroll snapshot for current month for reception1 (based on the seeded attendance only)
-- regular_pay = (regular_minutes/60) * hourly_rate
-- overtime_pay = (overtime_minutes/60) * overtime_hourly_rate
INSERT INTO payroll (
  staff_user_id, period_year, period_month,
  regular_minutes, overtime_minutes,
  hourly_rate_snapshot, overtime_hourly_rate_snapshot,
  regular_pay, overtime_pay,
  bonus, deductions,
  total_paid, paid_at
)
SELECT
  sp.user_id,
  YEAR(CURDATE()) AS period_year,
  MONTH(CURDATE()) AS period_month,

  COALESCE(SUM(a.regular_minutes),0) AS regular_minutes,
  COALESCE(SUM(a.overtime_minutes),0) AS overtime_minutes,

  sp.hourly_rate AS hourly_rate_snapshot,
  sp.overtime_hourly_rate AS overtime_hourly_rate_snapshot,

  ROUND((COALESCE(SUM(a.regular_minutes),0) / 60) * sp.hourly_rate, 2) AS regular_pay,
  ROUND((COALESCE(SUM(a.overtime_minutes),0) / 60) * sp.overtime_hourly_rate, 2) AS overtime_pay,

  0.00 AS bonus,
  0.00 AS deductions,

  ROUND(
    ((COALESCE(SUM(a.regular_minutes),0) / 60) * sp.hourly_rate) +
    ((COALESCE(SUM(a.overtime_minutes),0) / 60) * sp.overtime_hourly_rate),
    2
  ) AS total_paid,

  NULL AS paid_at
FROM staff_profiles sp
LEFT JOIN staff_attendance a
  ON a.staff_user_id = sp.user_id
 AND YEAR(a.work_date) = YEAR(CURDATE())
 AND MONTH(a.work_date) = MONTH(CURDATE())
WHERE sp.user_id = (SELECT id FROM users WHERE username='reception1')
GROUP BY sp.user_id, sp.hourly_rate, sp.overtime_hourly_rate;

-- =========================
-- CLEANING + MAINTENANCE REQUESTS (dynamic timestamps)
-- =========================
INSERT INTO cleaning_requests (room_id, reservation_id, requested_by_user_id, status, requested_at, completed_at, notes)
VALUES (
  (SELECT id FROM rooms WHERE room_number='102'),
  NULL,
  (SELECT id FROM users WHERE username='reception1'),
  'OPEN',
  DATE_SUB(NOW(), INTERVAL 2 HOUR),
  NULL,
  'Room 102 marked dirty - please clean'
);

INSERT INTO maintenance_requests (location_type, room_id, floor, description, requested_by_user_id, assigned_to_user_id, status, created_at, resolved_at)
VALUES (
  'ROOM',
  (SELECT id FROM rooms WHERE room_number='202'),
  NULL,
  'AC not cooling properly',
  (SELECT id FROM users WHERE username='reception1'),
  (SELECT id FROM users WHERE username='maint1'),
  'IN_PROGRESS',
  DATE_SUB(NOW(), INTERVAL 6 HOUR),
  NULL
);

-- =========================
-- SECURITY QUESTIONS (optional)
-- =========================
INSERT INTO security_questions (question_text, is_active)
VALUES
  ('What is the name of your first school?', 1),
  ('What is your favorite food?', 1),
  ('What city were you born in?', 1);

COMMIT;