-- ═══════════════════════════════════════════════════════════════════════════════
--  SmartStay PMS — Corrected Full Seed File
--  Matched to actual schema.sql column names
--
--  DB password: DB_Password123!   (from application.properties)
--  Passwords:
--    admin123   → admin
--    staff123   → all staff
--    client123  → all clients
--
--  Run:
--    docker exec -i smartstay-db mysql -u root -pDB_Password123! smartstay < seed.sql
-- ═══════════════════════════════════════════════════════════════════════════════

USE smartstay;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE reservation_services;
TRUNCATE TABLE invoice_lines;
TRUNCATE TABLE payments;
TRUNCATE TABLE invoices;
TRUNCATE TABLE cleaning_requests;
TRUNCATE TABLE maintenance_requests;
TRUNCATE TABLE staff_shift_assignments;
TRUNCATE TABLE staff_attendance;
TRUNCATE TABLE shifts;
TRUNCATE TABLE payroll;
TRUNCATE TABLE reservations;
TRUNCATE TABLE user_security_answers;
TRUNCATE TABLE password_reset_audit;
TRUNCATE TABLE password_reset_challenges;
TRUNCATE TABLE security_questions;
TRUNCATE TABLE room_images;
TRUNCATE TABLE services;
TRUNCATE TABLE staff_profiles;
TRUNCATE TABLE guests;
TRUNCATE TABLE rooms;
TRUNCATE TABLE room_types;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- ══════════════════════════════════════════════════════════════
-- 1. USERS
-- ══════════════════════════════════════════════════════════════
INSERT INTO users (username, email, password_hash, role, is_active, last_login_at, created_at) VALUES
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

-- ══════════════════════════════════════════════════════════════
-- 2. SECURITY QUESTIONS
-- ══════════════════════════════════════════════════════════════
INSERT INTO security_questions (question_text, is_active) VALUES
('What was your first pet''s name?',          1),
('What is your mother''s maiden name?',       1),
('What city were you born in?',               1),
('What was the name of your primary school?', 1),
('What is your oldest sibling''s middle name?', 1);

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

-- ══════════════════════════════════════════════════════════════
-- 3. ROOM TYPES
-- ══════════════════════════════════════════════════════════════
INSERT INTO room_types (name, description, price_per_night, max_occupancy, amenities) VALUES
('Single',       'Cozy room for solo travelers',                  350.00, 1, 'WiFi,Desk,TV,Safe'),
('Double',       'Comfortable room for couples',                  550.00, 2, 'WiFi,TV,Mini Bar,Safe'),
('Twin',         'Two separate beds, ideal for friends',          520.00, 2, 'WiFi,TV,Mini Bar'),
('Deluxe',       'Spacious deluxe room with premium fittings',    750.00, 3, 'WiFi,TV,Mini Bar,Safe,Bathtub'),
('Suite',        'Premium suite with lounge and kitchenette',     950.00, 4, 'WiFi,TV,Mini Bar,Jacuzzi,Bathtub,Lounge'),
('Presidential', 'Exclusive top-floor presidential suite',       2200.00, 6, 'WiFi,TV,Mini Bar,Jacuzzi,Bathtub,Lounge,Butler,Kitchen');

-- ══════════════════════════════════════════════════════════════
-- 4. ROOMS
-- ══════════════════════════════════════════════════════════════
INSERT INTO rooms (room_number, room_type_id, floor, status, notes) VALUES
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

-- ══════════════════════════════════════════════════════════════
-- 5. ROOM IMAGES
-- ══════════════════════════════════════════════════════════════
INSERT INTO room_images (room_id, image_path, is_primary, sort_order) VALUES
((SELECT id FROM rooms WHERE room_number='101'), '/images/rooms/101_main.jpg',    1, 1),
((SELECT id FROM rooms WHERE room_number='101'), '/images/rooms/101_bath.jpg',    0, 2),
((SELECT id FROM rooms WHERE room_number='102'), '/images/rooms/102_main.jpg',    1, 1),
((SELECT id FROM rooms WHERE room_number='201'), '/images/rooms/201_main.jpg',    1, 1),
((SELECT id FROM rooms WHERE room_number='201'), '/images/rooms/201_bath.jpg',    0, 2),
((SELECT id FROM rooms WHERE room_number='203'), '/images/rooms/203_main.jpg',    1, 1),
((SELECT id FROM rooms WHERE room_number='301'), '/images/rooms/301_main.jpg',    1, 1),
((SELECT id FROM rooms WHERE room_number='301'), '/images/rooms/301_view.jpg',    0, 2),
((SELECT id FROM rooms WHERE room_number='401'), '/images/rooms/401_main.jpg',    1, 1),
((SELECT id FROM rooms WHERE room_number='401'), '/images/rooms/401_jacuzzi.jpg', 0, 2),
((SELECT id FROM rooms WHERE room_number='402'), '/images/rooms/402_main.jpg',    1, 1),
((SELECT id FROM rooms WHERE room_number='501'), '/images/rooms/501_main.jpg',    1, 1),
((SELECT id FROM rooms WHERE room_number='501'), '/images/rooms/501_lounge.jpg',  0, 2),
((SELECT id FROM rooms WHERE room_number='502'), '/images/rooms/502_main.jpg',    1, 1);

-- ══════════════════════════════════════════════════════════════
-- 6. SERVICES  (schema: code, name, description, unit_price, is_active)
-- ══════════════════════════════════════════════════════════════
INSERT INTO services (code, name, description, unit_price, is_active) VALUES
('SVC-XFER',  'Airport Transfer',      'Round-trip airport pickup and drop-off',    250.00, 1),
('SVC-TOUR',  'City Tour',             'Full-day guided city tour (8h)',             400.00, 1),
('SVC-BKFT',  'Breakfast Buffet',      'Daily breakfast buffet per person',           85.00, 1),
('SVC-ROOM',  'Room Service',          'In-room dining (menu + 15% fee)',             50.00, 1),
('SVC-DINE',  'Romantic Dinner',       'Private candlelit dinner for two',           650.00, 1),
('SVC-LAUN',  'Laundry Standard',      'Same-day laundry per bag',                    80.00, 1),
('SVC-EXPR',  'Laundry Express',       '4-hour express laundry per bag',             130.00, 1),
('SVC-SPA1',  'Spa 60min',             'Full-body relaxation massage',               350.00, 1),
('SVC-SPA2',  'Spa 90min',             'Deep tissue massage — 90 minutes',           480.00, 1),
('SVC-FIT',   'Personal Trainer',      'One-hour private fitness session',           200.00, 1),
('SVC-COT',   'Baby Cot',              'Foldable cot with bedding per night',         60.00, 1),
('SVC-BED',   'Extra Bed',             'Additional bed setup per night',             120.00, 1),
('SVC-LATE',  'Late Checkout',         'Checkout extended to 14:00',                 150.00, 1),
('SVC-EARL',  'Early Checkin',         'Checkin from 08:00 instead of 14:00',        150.00, 1),
('SVC-CNF4',  'Conference Room (4h)',  'Private meeting room — half day',            500.00, 1),
('SVC-CNF8',  'Conference Room (8h)',  'Private meeting room — full day',            900.00, 1);

-- ══════════════════════════════════════════════════════════════
-- 7. GUESTS
-- ══════════════════════════════════════════════════════════════
INSERT INTO guests (first_name, last_name, email, phone, nationality, id_passport_number, preferences, created_at) VALUES
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
('Omar',    'Idrissi',  'omar.idrissi@gmail.com',  '+212688000015', 'Moroccan',  'MA555555', NULL,                                NOW());

-- ══════════════════════════════════════════════════════════════
-- 8. STAFF PROFILES
-- ══════════════════════════════════════════════════════════════
INSERT INTO staff_profiles (user_id, employee_code, position, department, hire_date, salary_base, emergency_contact, is_on_duty) VALUES
((SELECT id FROM users WHERE username='reception1'), 'EMP-REC-001', 'reception',   'Front Office', DATE_SUB(CURDATE(), INTERVAL 3 YEAR),  5200.00, '0600000001', 1),
((SELECT id FROM users WHERE username='reception2'), 'EMP-REC-002', 'reception',   'Front Office', DATE_SUB(CURDATE(), INTERVAL 1 YEAR),  4800.00, '0600000002', 1),
((SELECT id FROM users WHERE username='cleaning1'),  'EMP-CLN-001', 'cleaning',    'Housekeeping', DATE_SUB(CURDATE(), INTERVAL 2 YEAR),  3900.00, '0600000003', 1),
((SELECT id FROM users WHERE username='cleaning2'),  'EMP-CLN-002', 'cleaning',    'Housekeeping', DATE_SUB(CURDATE(), INTERVAL 6 MONTH), 3700.00, '0600000004', 0),
((SELECT id FROM users WHERE username='maint1'),     'EMP-MNT-001', 'maintenance', 'Engineering',  DATE_SUB(CURDATE(), INTERVAL 4 YEAR),  4600.00, '0600000005', 1),
((SELECT id FROM users WHERE username='maint2'),     'EMP-MNT-002', 'maintenance', 'Engineering',  DATE_SUB(CURDATE(), INTERVAL 8 MONTH), 4400.00, '0600000006', 0);

-- ══════════════════════════════════════════════════════════════
-- 9. SHIFTS  (schema: shift_name, start_time, end_time, is_night_shift)
-- ══════════════════════════════════════════════════════════════
INSERT INTO shifts (shift_name, start_time, end_time, is_night_shift) VALUES
('Morning Reception',    '06:00:00', '14:00:00', 0),
('Evening Reception',    '14:00:00', '22:00:00', 0),
('Night Reception',      '22:00:00', '06:00:00', 1),
('Morning Housekeeping', '07:00:00', '15:00:00', 0),
('Evening Housekeeping', '15:00:00', '23:00:00', 0),
('Morning Maintenance',  '08:00:00', '16:00:00', 0),
('OnCall Maintenance',   '16:00:00', '08:00:00', 1);

-- ══════════════════════════════════════════════════════════════
-- 10. SHIFT ASSIGNMENTS  (schema: staff_profile_id, shift_id, assigned_date, assigned_by_user_id)
-- ══════════════════════════════════════════════════════════════
INSERT INTO staff_shift_assignments (staff_profile_id, shift_id, assigned_date, assigned_by_user_id) VALUES
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-001'), (SELECT id FROM shifts WHERE shift_name='Morning Reception'),    DATE_SUB(CURDATE(),INTERVAL 5 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-001'), (SELECT id FROM shifts WHERE shift_name='Morning Reception'),    DATE_SUB(CURDATE(),INTERVAL 4 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-001'), (SELECT id FROM shifts WHERE shift_name='Morning Reception'),    DATE_SUB(CURDATE(),INTERVAL 3 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-001'), (SELECT id FROM shifts WHERE shift_name='Morning Reception'),    CURDATE(),                          (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-001'), (SELECT id FROM shifts WHERE shift_name='Morning Reception'),    DATE_ADD(CURDATE(),INTERVAL 1 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-002'), (SELECT id FROM shifts WHERE shift_name='Evening Reception'),    DATE_SUB(CURDATE(),INTERVAL 4 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-002'), (SELECT id FROM shifts WHERE shift_name='Evening Reception'),    CURDATE(),                          (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-002'), (SELECT id FROM shifts WHERE shift_name='Evening Reception'),    DATE_ADD(CURDATE(),INTERVAL 1 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'), (SELECT id FROM shifts WHERE shift_name='Morning Housekeeping'), DATE_SUB(CURDATE(),INTERVAL 3 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'), (SELECT id FROM shifts WHERE shift_name='Morning Housekeeping'), DATE_SUB(CURDATE(),INTERVAL 2 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'), (SELECT id FROM shifts WHERE shift_name='Morning Housekeeping'), CURDATE(),                          (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'), (SELECT id FROM shifts WHERE shift_name='Morning Maintenance'),  DATE_SUB(CURDATE(),INTERVAL 4 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'), (SELECT id FROM shifts WHERE shift_name='Morning Maintenance'),  DATE_SUB(CURDATE(),INTERVAL 2 DAY), (SELECT id FROM users WHERE username='admin')),
((SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'), (SELECT id FROM shifts WHERE shift_name='Morning Maintenance'),  CURDATE(),                          (SELECT id FROM users WHERE username='admin'));

-- ══════════════════════════════════════════════════════════════
-- 11. RESERVATIONS
-- Schema: guest_id, room_id, booked_by_user_id, reservation_code,
--         check_in_date, check_out_date, adults_count, children_count,
--         status, special_requests
-- ══════════════════════════════════════════════════════════════
INSERT INTO reservations (guest_id, room_id, booked_by_user_id, reservation_code, check_in_date, check_out_date, adults_count, children_count, status, special_requests) VALUES
-- Checked out (past)
((SELECT id FROM guests WHERE email='alice.martin@mail.com'),  (SELECT id FROM rooms WHERE room_number='101'), (SELECT id FROM users WHERE username='client1'),    'RES-2025-0001', DATE_SUB(CURDATE(),INTERVAL 20 DAY), DATE_SUB(CURDATE(),INTERVAL 17 DAY), 1, 0, 'CHECKED_OUT', 'High floor please'),
((SELECT id FROM guests WHERE email='bob.jones@mail.com'),     (SELECT id FROM rooms WHERE room_number='201'), (SELECT id FROM users WHERE username='client2'),    'RES-2025-0002', DATE_SUB(CURDATE(),INTERVAL 15 DAY), DATE_SUB(CURDATE(),INTERVAL 12 DAY), 2, 0, 'CHECKED_OUT', NULL),
((SELECT id FROM guests WHERE email='clara.santos@mail.com'),  (SELECT id FROM rooms WHERE room_number='401'), (SELECT id FROM users WHERE username='client3'),    'RES-2025-0003', DATE_SUB(CURDATE(),INTERVAL 10 DAY), DATE_SUB(CURDATE(),INTERVAL  7 DAY), 2, 0, 'CHECKED_OUT', 'Sea view preferred'),
((SELECT id FROM guests WHERE email='david.kim@mail.com'),     (SELECT id FROM rooms WHERE room_number='303'), (SELECT id FROM users WHERE username='client4'),    'RES-2025-0004', DATE_SUB(CURDATE(),INTERVAL  8 DAY), DATE_SUB(CURDATE(),INTERVAL  5 DAY), 1, 0, 'CHECKED_OUT', 'Late checkout needed'),
((SELECT id FROM guests WHERE email='karim.alami@gmail.com'),  (SELECT id FROM rooms WHERE room_number='103'), (SELECT id FROM users WHERE username='reception1'), 'RES-2025-0005', DATE_SUB(CURDATE(),INTERVAL  6 DAY), DATE_SUB(CURDATE(),INTERVAL  4 DAY), 1, 0, 'CHECKED_OUT', NULL),
-- Currently checked in
((SELECT id FROM guests WHERE email='hiro.tanaka@mail.com'),   (SELECT id FROM rooms WHERE room_number='102'), (SELECT id FROM users WHERE username='client8'),    'RES-2025-0006', DATE_SUB(CURDATE(),INTERVAL  2 DAY), DATE_ADD(CURDATE(),INTERVAL  3 DAY), 1, 0, 'CHECKED_IN',  'Early breakfast daily'),
((SELECT id FROM guests WHERE email='farid.benali@mail.com'),  (SELECT id FROM rooms WHERE room_number='202'), (SELECT id FROM users WHERE username='client6'),    'RES-2025-0007', DATE_SUB(CURDATE(),INTERVAL  1 DAY), DATE_ADD(CURDATE(),INTERVAL  4 DAY), 2, 1, 'CHECKED_IN',  NULL),
((SELECT id FROM guests WHERE email='lena.muller@gmail.com'),  (SELECT id FROM rooms WHERE room_number='302'), (SELECT id FROM users WHERE username='reception1'), 'RES-2025-0008', DATE_SUB(CURDATE(),INTERVAL  3 DAY), DATE_ADD(CURDATE(),INTERVAL  2 DAY), 2, 0, 'CHECKED_IN',  'City view essential'),
((SELECT id FROM guests WHERE email='james.osei@mail.com'),    (SELECT id FROM rooms WHERE room_number='402'), (SELECT id FROM users WHERE username='client10'),   'RES-2025-0009', CURDATE(),                          DATE_ADD(CURDATE(),INTERVAL  5 DAY), 1, 0, 'CHECKED_IN',  'VIP treatment'),
((SELECT id FROM guests WHERE email='nadia.chraibi@gmail.com'),(SELECT id FROM rooms WHERE room_number='502'), (SELECT id FROM users WHERE username='reception2'), 'RES-2025-0010', DATE_SUB(CURDATE(),INTERVAL  5 DAY), DATE_ADD(CURDATE(),INTERVAL 10 DAY), 2, 2, 'CHECKED_IN',  'Butler on call 24/7'),
-- Future confirmed
((SELECT id FROM guests WHERE email='emma.wilson@mail.com'),   (SELECT id FROM rooms WHERE room_number='203'), (SELECT id FROM users WHERE username='client5'),    'RES-2025-0011', DATE_ADD(CURDATE(),INTERVAL  2 DAY), DATE_ADD(CURDATE(),INTERVAL  6 DAY), 2, 0, 'CONFIRMED',   'Vegetarian menu'),
((SELECT id FROM guests WHERE email='grace.okafor@mail.com'),  (SELECT id FROM rooms WHERE room_number='301'), (SELECT id FROM users WHERE username='client7'),    'RES-2025-0012', DATE_ADD(CURDATE(),INTERVAL  5 DAY), DATE_ADD(CURDATE(),INTERVAL  9 DAY), 1, 0, 'CONFIRMED',   'No feather pillows'),
((SELECT id FROM guests WHERE email='omar.idrissi@gmail.com'), (SELECT id FROM rooms WHERE room_number='104'), (SELECT id FROM users WHERE username='reception1'), 'RES-2025-0013', DATE_ADD(CURDATE(),INTERVAL  1 DAY), DATE_ADD(CURDATE(),INTERVAL  3 DAY), 1, 0, 'CONFIRMED',   NULL),
((SELECT id FROM guests WHERE email='alice.martin@mail.com'),  (SELECT id FROM rooms WHERE room_number='501'), (SELECT id FROM users WHERE username='client1'),    'RES-2025-0014', DATE_ADD(CURDATE(),INTERVAL 10 DAY), DATE_ADD(CURDATE(),INTERVAL 14 DAY), 2, 0, 'CONFIRMED',   'Return guest — VIP'),
-- Cancelled
((SELECT id FROM guests WHERE email='ines.dupont@mail.com'),   (SELECT id FROM rooms WHERE room_number='205'), (SELECT id FROM users WHERE username='client9'),    'RES-2025-0015', DATE_ADD(CURDATE(),INTERVAL  3 DAY), DATE_ADD(CURDATE(),INTERVAL  7 DAY), 2, 2, 'CANCELLED',   'Family room'),
((SELECT id FROM guests WHERE email='bob.jones@mail.com'),     (SELECT id FROM rooms WHERE room_number='403'), (SELECT id FROM users WHERE username='client2'),    'RES-2025-0016', DATE_ADD(CURDATE(),INTERVAL  1 DAY), DATE_ADD(CURDATE(),INTERVAL  3 DAY), 1, 0, 'CANCELLED',   NULL);

-- ══════════════════════════════════════════════════════════════
-- 12. RESERVATION SERVICES
-- ══════════════════════════════════════════════════════════════
INSERT INTO reservation_services (reservation_id, service_id, quantity, unit_price, requested_at) VALUES
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0001'), (SELECT id FROM services WHERE code='SVC-BKFT'), 3,  85.00, DATE_SUB(NOW(), INTERVAL 20 DAY)),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0001'), (SELECT id FROM services WHERE code='SVC-XFER'), 1, 250.00, DATE_SUB(NOW(), INTERVAL 20 DAY)),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0006'), (SELECT id FROM services WHERE code='SVC-BKFT'), 2,  85.00, DATE_SUB(NOW(), INTERVAL  2 DAY)),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0006'), (SELECT id FROM services WHERE code='SVC-SPA1'), 1, 350.00, DATE_SUB(NOW(), INTERVAL  1 DAY)),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0009'), (SELECT id FROM services WHERE code='SVC-DINE'), 1, 650.00, NOW()),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0009'), (SELECT id FROM services WHERE code='SVC-LATE'), 1, 150.00, NOW()),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0009'), (SELECT id FROM services WHERE code='SVC-CNF4'), 1, 500.00, NOW()),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0010'), (SELECT id FROM services WHERE code='SVC-BKFT'), 15, 85.00, DATE_SUB(NOW(), INTERVAL 5 DAY)),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0010'), (SELECT id FROM services WHERE code='SVC-SPA2'), 3, 480.00, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0010'), (SELECT id FROM services WHERE code='SVC-TOUR'), 2, 400.00, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ══════════════════════════════════════════════════════════════
-- 13. INVOICES (schema: invoice_number, subtotal_amount, tax_amount, total_amount, status)
-- ══════════════════════════════════════════════════════════════
INSERT INTO invoices (reservation_id, invoice_number, issued_at, subtotal_amount, tax_amount, total_amount, status, notes) VALUES
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0001'), 'INV-2025-0001', DATE_SUB(NOW(),INTERVAL 17 DAY), 1305.00, 130.50, 1435.50, 'PAID', NULL),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0002'), 'INV-2025-0002', DATE_SUB(NOW(),INTERVAL 12 DAY), 1650.00, 165.00, 1815.00, 'PAID', NULL),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0003'), 'INV-2025-0003', DATE_SUB(NOW(),INTERVAL  7 DAY), 2850.00, 285.00, 3135.00, 'PAID', 'Sea view supplement'),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0004'), 'INV-2025-0004', DATE_SUB(NOW(),INTERVAL  5 DAY), 2400.00, 240.00, 2640.00, 'PAID', 'Late checkout charged'),
((SELECT id FROM reservations WHERE reservation_code='RES-2025-0005'), 'INV-2025-0005', DATE_SUB(NOW(),INTERVAL  4 DAY),  700.00,  70.00,  770.00, 'PAID', NULL);

-- ══════════════════════════════════════════════════════════════
-- 14. PAYMENTS  (schema: method ENUM('CASH','CARD','TRANSFER'), transaction_ref, status)
-- ══════════════════════════════════════════════════════════════
INSERT INTO payments (invoice_id, amount, method, paid_at, transaction_ref, status) VALUES
((SELECT id FROM invoices WHERE invoice_number='INV-2025-0001'), 1435.50, 'CARD',     DATE_SUB(NOW(),INTERVAL 17 DAY), 'TXN-CC-00001', 'SUCCESS'),
((SELECT id FROM invoices WHERE invoice_number='INV-2025-0002'), 1815.00, 'CASH',     DATE_SUB(NOW(),INTERVAL 12 DAY), NULL,           'SUCCESS'),
((SELECT id FROM invoices WHERE invoice_number='INV-2025-0003'), 3135.00, 'CARD',     DATE_SUB(NOW(),INTERVAL  7 DAY), 'TXN-CC-00003', 'SUCCESS'),
((SELECT id FROM invoices WHERE invoice_number='INV-2025-0004'), 2640.00, 'TRANSFER', DATE_SUB(NOW(),INTERVAL  5 DAY), 'TXN-BT-00004', 'SUCCESS'),
((SELECT id FROM invoices WHERE invoice_number='INV-2025-0005'),  770.00, 'CASH',     DATE_SUB(NOW(),INTERVAL  4 DAY), NULL,           'SUCCESS');

-- ══════════════════════════════════════════════════════════════
-- 15. CLEANING REQUESTS
-- Schema: room_id, requested_by_user_id, assigned_to_staff_id,
--         reservation_id, priority, status, request_note,
--         created_at, started_at, completed_at
-- ══════════════════════════════════════════════════════════════
INSERT INTO cleaning_requests (room_id, requested_by_user_id, assigned_to_staff_id, reservation_id, priority, status, request_note, created_at, started_at, completed_at) VALUES
((SELECT id FROM rooms WHERE room_number='101'),
 (SELECT id FROM users WHERE username='reception1'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'),
 (SELECT id FROM reservations WHERE reservation_code='RES-2025-0001'),
 'HIGH', 'DONE', 'Post checkout deep clean',
 DATE_SUB(NOW(),INTERVAL 17 DAY), DATE_SUB(NOW(),INTERVAL 17 DAY), DATE_SUB(NOW(),INTERVAL 17 DAY) + INTERVAL 2 HOUR),

((SELECT id FROM rooms WHERE room_number='201'),
 (SELECT id FROM users WHERE username='reception1'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'),
 (SELECT id FROM reservations WHERE reservation_code='RES-2025-0002'),
 'MEDIUM', 'DONE', NULL,
 DATE_SUB(NOW(),INTERVAL 12 DAY), DATE_SUB(NOW(),INTERVAL 12 DAY), DATE_SUB(NOW(),INTERVAL 12 DAY) + INTERVAL 90 MINUTE),

((SELECT id FROM rooms WHERE room_number='104'),
 (SELECT id FROM users WHERE username='reception2'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'),
 NULL,
 'MEDIUM', 'IN_PROGRESS', 'Guest requested extra towels',
 NOW(), NOW(), NULL),

((SELECT id FROM rooms WHERE room_number='304'),
 (SELECT id FROM users WHERE username='reception1'),
 NULL, NULL,
 'HIGH', 'NEW', 'VIP arrival tonight — must be perfect',
 NOW(), NULL, NULL),

((SELECT id FROM rooms WHERE room_number='102'),
 (SELECT id FROM users WHERE username='reception2'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'),
 (SELECT id FROM reservations WHERE reservation_code='RES-2025-0006'),
 'MEDIUM', 'NEW', 'Daily service',
 NOW(), NULL, NULL);

-- ══════════════════════════════════════════════════════════════
-- 16. MAINTENANCE REQUESTS
-- Schema: room_id, reported_by_user_id, assigned_to_staff_id,
--         priority, status, title, description, created_at, resolved_at
-- ══════════════════════════════════════════════════════════════
INSERT INTO maintenance_requests (room_id, reported_by_user_id, assigned_to_staff_id, priority, status, title, description, created_at, resolved_at) VALUES
((SELECT id FROM rooms WHERE room_number='204'),
 (SELECT id FROM users WHERE username='reception1'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'),
 'HIGH', 'RESOLVED', 'AC not cooling',
 'AC not cooling below 24C. Refrigerant topped up.',
 DATE_SUB(NOW(),INTERVAL 7 DAY), DATE_SUB(NOW(),INTERVAL 6 DAY)),

((SELECT id FROM rooms WHERE room_number='204'),
 (SELECT id FROM users WHERE username='cleaning1'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'),
 'MEDIUM', 'IN_PROGRESS', 'Shower drain blocked',
 'Shower drain blocked — slow drainage.',
 DATE_SUB(NOW(),INTERVAL 1 DAY), NULL),

((SELECT id FROM rooms WHERE room_number='403'),
 (SELECT id FROM users WHERE username='reception2'),
 (SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'),
 'URGENT', 'NEW', 'AC compressor fault',
 'AC compressor fault — complete unit replacement needed.',
 NOW(), NULL),

((SELECT id FROM rooms WHERE room_number='301'),
 (SELECT id FROM users WHERE username='cleaning2'),
 NULL,
 'MEDIUM', 'NEW', 'Bedside lamp flickering',
 'Bedside lamp flickering — probable loose wire.',
 NOW(), NULL);

-- ══════════════════════════════════════════════════════════════
-- 17. PAYROLL  (schema: staff_profile_id, period_start, period_end,
--               base_salary, bonuses, deductions, net_salary, status, paid_at)
-- ══════════════════════════════════════════════════════════════
INSERT INTO payroll (staff_profile_id, period_start, period_end, base_salary, bonuses, deductions, net_salary, status, paid_at) VALUES
((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-001'),
 DATE_FORMAT(DATE_SUB(CURDATE(),INTERVAL 1 MONTH),'%Y-%m-01'),
 LAST_DAY(DATE_SUB(CURDATE(),INTERVAL 1 MONTH)),
 5200.00, 500.00, 300.00, 5400.00, 'PAID', DATE_SUB(NOW(),INTERVAL 5 DAY)),

((SELECT id FROM staff_profiles WHERE employee_code='EMP-REC-002'),
 DATE_FORMAT(DATE_SUB(CURDATE(),INTERVAL 1 MONTH),'%Y-%m-01'),
 LAST_DAY(DATE_SUB(CURDATE(),INTERVAL 1 MONTH)),
 4800.00, 0.00, 250.00, 4550.00, 'PAID', DATE_SUB(NOW(),INTERVAL 5 DAY)),

((SELECT id FROM staff_profiles WHERE employee_code='EMP-CLN-001'),
 DATE_FORMAT(DATE_SUB(CURDATE(),INTERVAL 1 MONTH),'%Y-%m-01'),
 LAST_DAY(DATE_SUB(CURDATE(),INTERVAL 1 MONTH)),
 3900.00, 200.00, 200.00, 3900.00, 'PAID', DATE_SUB(NOW(),INTERVAL 5 DAY)),

((SELECT id FROM staff_profiles WHERE employee_code='EMP-MNT-001'),
 DATE_FORMAT(DATE_SUB(CURDATE(),INTERVAL 1 MONTH),'%Y-%m-01'),
 LAST_DAY(DATE_SUB(CURDATE(),INTERVAL 1 MONTH)),
 4600.00, 300.00, 250.00, 4650.00, 'PAID', DATE_SUB(NOW(),INTERVAL 5 DAY));

-- ══════════════════════════════════════════════════════════════
-- VERIFY
-- ══════════════════════════════════════════════════════════════
SELECT 'users'          AS tbl, COUNT(*) AS n FROM users
UNION ALL SELECT 'rooms',         COUNT(*) FROM rooms
UNION ALL SELECT 'reservations',  COUNT(*) FROM reservations
UNION ALL SELECT 'guests',        COUNT(*) FROM guests
UNION ALL SELECT 'services',      COUNT(*) FROM services
UNION ALL SELECT 'invoices',      COUNT(*) FROM invoices
UNION ALL SELECT 'payments',      COUNT(*) FROM payments
UNION ALL SELECT 'cleaning_reqs', COUNT(*) FROM cleaning_requests
UNION ALL SELECT 'maint_reqs',    COUNT(*) FROM maintenance_requests
UNION ALL SELECT 'payroll',       COUNT(*) FROM payroll;