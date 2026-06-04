-- ═══════════════════════════════════════════════════════════════════════
--  SmartStay PMS — Minimal Seed (fast startup)
-- ═══════════════════════════════════════════════════════════════════════

USE smartstay;

-- 1. USERS
INSERT INTO users (username, email, password_hash, role, is_active, last_login_at, created_at) VALUES
('admin',      'admin@smartstay.local',   '$2a$10$3JgR5c6DWukENJ3UeyDmZeAvviMQnPyVVkCEmvhVjbl0zMOvDz7kO', 'ADMIN',  1, DATE_SUB(NOW(), INTERVAL 1 HOUR),    DATE_SUB(NOW(), INTERVAL 60 DAY)),
('reception1', 'rec1@smartstay.local',    '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, DATE_SUB(NOW(), INTERVAL 2 HOUR),    DATE_SUB(NOW(), INTERVAL 55 DAY)),
('cleaning1',  'clean1@smartstay.local',  '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, DATE_SUB(NOW(), INTERVAL 3 HOUR),    DATE_SUB(NOW(), INTERVAL 48 DAY)),
('maint1',     'maint1@smartstay.local',  '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, DATE_SUB(NOW(), INTERVAL 5 HOUR),    DATE_SUB(NOW(), INTERVAL 45 DAY)),
('client1',    'alice.martin@mail.com',   '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR),    DATE_SUB(NOW(), INTERVAL 30 DAY));

-- 2. STAFF PROFILES
INSERT INTO staff_profiles (user_id, employee_code, position, department, hire_date, salary_base, emergency_contact, is_on_duty) VALUES
((SELECT id FROM users WHERE username='reception1'), 'EMP-REC-001', 'Receptionist', 'Front Desk',   DATE_SUB(CURDATE(), INTERVAL 420 DAY), 4500.00, '0670000001', 1),
((SELECT id FROM users WHERE username='cleaning1'),  'EMP-CLN-001', 'Cleaner',      'Housekeeping', DATE_SUB(CURDATE(), INTERVAL 380 DAY), 3800.00, '0670000002', 0),
((SELECT id FROM users WHERE username='maint1'),     'EMP-MNT-001', 'Technician',   'Maintenance',  DATE_SUB(CURDATE(), INTERVAL 500 DAY), 5200.00, '0670000003', 0);

-- 3. ROOM TYPES
INSERT INTO room_types (name, description, price_per_night, max_occupancy, amenities) VALUES
('Single', 'Cozy room for solo travelers',               350.00, 1, 'WiFi,Desk,TV,Safe'),
('Double', 'Comfortable room for couples',               550.00, 2, 'WiFi,TV,Mini Bar,Safe'),
('Deluxe', 'Spacious deluxe room with premium fittings', 750.00, 3, 'WiFi,TV,Mini Bar,Safe,Bathtub');

-- 4. ROOMS
INSERT INTO rooms (room_number, room_type_id, floor, status, notes) VALUES
('101', (SELECT id FROM room_types WHERE name='Single'), 1, 'AVAILABLE', 'Near elevator'),
('102', (SELECT id FROM room_types WHERE name='Single'), 1, 'OCCUPIED',  NULL),
('201', (SELECT id FROM room_types WHERE name='Double'), 2, 'AVAILABLE', NULL),
('202', (SELECT id FROM room_types WHERE name='Double'), 2, 'CLEANING',  NULL),
('301', (SELECT id FROM room_types WHERE name='Deluxe'), 3, 'AVAILABLE', 'City view'),
('302', (SELECT id FROM room_types WHERE name='Deluxe'), 3, 'MAINTENANCE', 'AC inspection');

-- 5. ROOM IMAGES
INSERT INTO room_images (room_id, image_path, is_primary, sort_order) VALUES
((SELECT id FROM rooms WHERE room_number='101'), '/images/rooms/king.jpg', 1, 1),
((SELECT id FROM rooms WHERE room_number='102'), '/images/rooms/king.jpg', 1, 1),
((SELECT id FROM rooms WHERE room_number='201'), '/images/rooms/double.jpg', 1, 1),
((SELECT id FROM rooms WHERE room_number='202'), '/images/rooms/double.jpg', 1, 1),
((SELECT id FROM rooms WHERE room_number='301'), '/images/rooms/twin_king.jpg', 1, 1),
((SELECT id FROM rooms WHERE room_number='302'), '/images/rooms/twin_king.jpg', 1, 1);

-- 6. GUESTS (small sample)
INSERT INTO guests (first_name, last_name, email, phone, nationality, id_passport_number, preferences, created_at) VALUES
('Alice', 'Martin', 'alice.martin@mail.com', '+212611000001', 'French',  'FR123456', 'High floor, quiet', DATE_SUB(NOW(), INTERVAL 30 DAY)),
('Bob',   'Jones',  'bob.jones@mail.com',   '+212611000002', 'British', 'GB234567', 'No smoking',        DATE_SUB(NOW(), INTERVAL 28 DAY)),
('Clara', 'Santos', 'clara.santos@mail.com','+212611000003', 'Spanish', 'ES345678', 'Sea view',          DATE_SUB(NOW(), INTERVAL 25 DAY));

-- 7. RESERVATIONS (small sample)
INSERT INTO reservations (guest_id, room_id, booked_by_user_id, reservation_code,
                          check_in_date, check_out_date, adults_count, children_count, status, special_requests) VALUES
((SELECT id FROM guests WHERE email='alice.martin@mail.com'), (SELECT id FROM rooms WHERE room_number='101'),
 (SELECT id FROM users WHERE username='client1'), 'RES-0001', DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
 1, 0, 'CHECKED_IN', NULL),
((SELECT id FROM guests WHERE email='bob.jones@mail.com'), (SELECT id FROM rooms WHERE room_number='201'),
 (SELECT id FROM users WHERE username='reception1'), 'RES-0002', DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY),
 2, 0, 'CONFIRMED', 'Late checkout');

-- 8. SERVICES (small sample)
INSERT INTO services (code, name, description, unit_price, is_active) VALUES
('SVC-BKFT', 'Breakfast Buffet', 'Daily breakfast buffet per person', 85.00, 1),
('SVC-XFER', 'Airport Transfer', 'Round-trip airport pickup and drop-off', 250.00, 1);

-- 9. SHIFTS (small sample)
INSERT INTO shifts (shift_name, start_time, end_time, is_night_shift) VALUES
('Morning', '08:00:00', '16:00:00', 0),
('Evening', '16:00:00', '00:00:00', 0),
('Night',   '00:00:00', '08:00:00', 1);
