USE Smart_Hotel_v_1_0;

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

INSERT INTO users (username, email, password_hash, role, is_active, last_login_at, created_at)
VALUES
  ('admin',      'admin@smartstay.local',      '$2a$10$3JgR5c6DWukENJ3UeyDmZeAvviMQnPyVVkCEmvhVjbl0zMOvDz7kO', 'ADMIN',  1, DATE_SUB(NOW(), INTERVAL 2 DAY),  DATE_SUB(NOW(), INTERVAL 20 DAY)),
  ('reception1', 'reception1@smartstay.local', '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_SUB(NOW(), INTERVAL 18 DAY)),
  ('cleaning1',  'cleaning1@smartstay.local',  '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, NULL,                             DATE_SUB(NOW(), INTERVAL 18 DAY)),
  ('maint1',     'maint1@smartstay.local',     '$2a$10$dnyOescThl9OhLkabMnWLuHShLb47DGYFJxkSgSm8DP4q9ff.YcgO', 'STAFF',  1, NULL,                             DATE_SUB(NOW(), INTERVAL 18 DAY)),
  ('client1',    'client1@mail.com',           '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 10 DAY)),
  ('client2',    'client2@mail.com',           '$2a$10$Fv9wGZ9vUyeTDHd35K5eQeiXjU5S.pwj5cn3/ct2mUavg9ux4ikcu', 'CLIENT', 1, NULL,                             DATE_SUB(NOW(), INTERVAL 12 DAY));

INSERT INTO security_questions (question_text, is_active)
VALUES
  ('What was your first pet''s name?', 1),
  ('What is your mother''s maiden name?', 1),
  ('What city were you born in?', 1);

INSERT INTO user_security_answers (user_id, question_id, answer_hash, created_at)
VALUES
  ((SELECT id FROM users WHERE username='admin'),      1, SHA2('tiger',256), NOW()),
  ((SELECT id FROM users WHERE username='reception1'), 2, SHA2('smith',256), NOW()),
  ((SELECT id FROM users WHERE username='client1'),    3, SHA2('rabat',256), NOW());

INSERT INTO room_types (name, description, price_per_night, max_occupancy, amenities)
VALUES
  ('Single', 'Cozy room for solo travelers', 350.00, 1, 'WiFi,Desk,TV'),
  ('Double', 'Comfortable room for couples', 550.00, 2, 'WiFi,TV,Mini Bar'),
  ('Suite',  'Premium suite with lounge area', 950.00, 4, 'WiFi,TV,Mini Bar,Jacuzzi');

INSERT INTO rooms (room_number, room_type_id, floor, status, notes)
VALUES
  ('101', (SELECT id FROM room_types WHERE name='Single'), 1, 'AVAILABLE', 'Near elevator'),
  ('102', (SELECT id FROM room_types WHERE name='Single'), 1, 'AVAILABLE', NULL),
  ('201', (SELECT id FROM room_types WHERE name='Double'), 2, 'OCCUPIED',  NULL),
  ('202', (SELECT id FROM room_types WHERE name='Double'), 2, 'MAINTENANCE','AC issue reported'),
  ('301', (SELECT id FROM room_types WHERE name='Suite'),  3, 'AVAILABLE', 'Sea view');

INSERT INTO room_images (room_id, image_path, is_primary, sort_order)
VALUES
  ((SELECT id FROM rooms WHERE room_number='101'), '/images/rooms/101_1.jpg', 1, 1),
  ((SELECT id FROM rooms WHERE room_number='101'), '/images/rooms/101_2.jpg', 0, 2),
  ((SELECT id FROM rooms WHERE room_number='201'), '/images/rooms/201_1.jpg', 1, 1),
  ((SELECT id FROM rooms WHERE room_number='201'), '/images/rooms/201_2.jpg', 0, 2),
  ((SELECT id FROM rooms WHERE room_number='202'), '/images/rooms/202_1.jpg', 1, 1),
  ((SELECT id FROM rooms WHERE room_number='301'), '/images/rooms/301_1.jpg', 1, 1),
  ((SELECT id FROM rooms WHERE room_number='301'), '/images/rooms/301_2.jpg', 0, 2);

INSERT INTO guests (first_name, last_name, email, phone, nationality, id_passport_number, preferences, created_at)
VALUES
  ('Luffy', 'D.', 'luffy@mail.com', '+212600000001', 'Moroccan', 'P123456', 'High floor', NOW()),
  ('Nami',  'C.', 'nami@mail.com',  '+212600000003', 'Moroccan', 'P345678', 'Extra pillows', NOW());

INSERT INTO staff_profiles (user_id, employee_code, position, department, hire_date, salary_base, emergency_contact, is_on_duty)
VALUES
  ((SELECT id FROM users WHERE username='reception1'), 'EMP-REC-001', 'reception',   'Front Office', DATE_SUB(CURDATE(), INTERVAL 2 YEAR), 5000.00, '0600000001', 1),
  ((SELECT id FROM users WHERE username='cleaning1'),  'EMP-CLN-001', 'cleaning',    'Housekeeping', DATE_SUB(CURDATE(), INTERVAL 1 YEAR), 3800.00, '0600000002', 1),
  ((SELECT id FROM users WHERE username='maint1'),     'EMP-MNT-001', 'maintenance', 'Engineering',  DATE_SUB(CURDATE(), INTERVAL 3 YEAR), 4500.00, '0600000003', 0);