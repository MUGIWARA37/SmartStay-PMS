PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;

-- =========================================
-- Optional metadata tables (if not in schema, skip this section)
-- =========================================
CREATE TABLE IF NOT EXISTS hotel_info (
  name TEXT PRIMARY KEY,
  address TEXT NOT NULL,
  star_rating INTEGER NOT NULL,
  total_rooms INTEGER NOT NULL,
  amenities_json TEXT NOT NULL,
  check_in_time TEXT NOT NULL,
  check_out_time TEXT NOT NULL,
  currency TEXT NOT NULL,
  contact TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS floor_wings (
  floor INTEGER PRIMARY KEY,
  wing TEXT NOT NULL,
  theme TEXT NOT NULL
);



INSERT OR REPLACE INTO hotel_info
(name, address, star_rating, total_rooms, amenities_json, check_in_time, check_out_time, currency, contact)
VALUES
(
  'The Grand Meridian',
  '1450 Meridian Avenue, Downtown, Miami, FL 33131, USA',
  5,
  80,
  '["Spa","Rooftop Pool","Fine Dining","Business Center","Gym","Valet Parking","Conference Halls","Airport Transfer","24/7 Concierge","High-Speed WiFi"]',
  '15:00',
  '11:00',
  'USD',
  '+1-305-555-0100'
);

INSERT OR REPLACE INTO floor_wings (floor, wing, theme) VALUES
(1, 'Lobby Wing', 'Arrival & Comfort'),
(2, 'Garden Wing', 'Botanical Serenity'),
(3, 'Executive Wing', 'Urban Prestige'),
(4, 'Premium Wing', 'Skyline Leisure'),
(5, 'Penthouse Wing', 'Crown Collection');

-- =========================================
-- USERS (for staff + some guests with accounts)
-- Note: adapt password_hash values as needed
-- =========================================
INSERT OR IGNORE INTO users (id, username, password_hash, role, first_name, last_name, email, phone) VALUES
(1001, 'carlos.mendes', 'HASH_ST_001', 'STAFF', 'Carlos', 'Mendes', 'carlos.mendes@grandmeridian.com', '+1-555-0301'),
(1002, 'huda.alami',    'HASH_ST_002', 'STAFF', 'Huda', 'Alami', 'huda.alami@grandmeridian.com', '+1-555-0302'),
(1003, 'ethan.brooks',  'HASH_ST_003', 'STAFF', 'Ethan', 'Brooks', 'ethan.brooks@grandmeridian.com', '+1-555-0303'),
(1004, 'mireille.ndour','HASH_ST_004', 'STAFF', 'Mireille', 'N''Dour', 'mireille.ndour@grandmeridian.com', '+1-555-0304'),
(1005, 'rafael.costa',  'HASH_ST_005', 'STAFF', 'Rafael', 'Costa', 'rafael.costa@grandmeridian.com', '+1-555-0305');

INSERT OR IGNORE INTO staff_profiles (user_id, position, hire_date, active) VALUES
(1001, 'manager', '2021-06-01', 1),
(1002, 'receptionist', '2022-01-15', 1),
(1003, 'receptionist', '2020-09-10', 1),
(1004, 'concierge', '2019-11-21', 1),
(1005, 'security', '2021-03-18', 1);

-- =========================================
-- SHIFTS
-- =========================================
INSERT OR IGNORE INTO shifts (id, name, start_time, end_time) VALUES
(1, 'MORNING', '07:00:00', '15:00:00'),
(2, 'MIDDAY',  '15:00:00', '23:00:00'),
(3, 'NIGHT',   '23:00:00', '07:00:00');

-- =========================================
-- ROOM TYPES (map JSON types to your schema types)
-- =========================================
INSERT OR IGNORE INTO room_types (id, name, base_rate, max_occupancy, description) VALUES
(11, 'Standard Single', 160.00, 1, 'Standard single room'),
(12, 'Deluxe Double',   245.00, 2, 'Deluxe double room'),
(13, 'Junior Suite',    370.00, 3, 'Junior suite'),
(14, 'Executive Suite', 575.00, 4, 'Executive suite'),
(15, 'Penthouse',      1225.00, 6, 'Luxury penthouse'),
(16, 'Accessible Room', 195.00, 2, 'Accessible guest room');

-- =========================================
-- ROOMS
-- IMPORTANT:
-- your rooms table has limited columns, so JSON-only fields
-- (wing, bed_type, amenities, size_sqm, floor_plan_url, etc.)
-- should go into `notes` as JSON blob if you keep current schema.
-- =========================================

INSERT OR REPLACE INTO rooms (id, room_number, room_type_id, status, floor, notes) VALUES
(101, '101', 16, 'OCCUPIED', 1, '{"room_id":"F1-101","wing":"Lobby Wing","bed_type":"Queen","view":"courtyard","amenities":["WiFi","minibar","safe","AC","roll-in shower","grab bars"],"size_sqm":30,"smoking":false,"accessible":true,"floor_plan_url":"https://example.com/plans/101.png","last_cleaned":"2026-04-06T08:10:00Z","maintenance_notes":null}'),
(102, '102', 11, 'OCCUPIED', 1, '{"room_id":"F1-102","wing":"Lobby Wing","bed_type":"Single","view":"none","amenities":["WiFi","safe","AC","smart_tv"],"size_sqm":21,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/102.png","last_cleaned":"2026-04-06T09:00:00Z","maintenance_notes":null}'),
(103, '103', 11, 'AVAILABLE', 1, '{"room_id":"F1-103","wing":"Lobby Wing","bed_type":"Single","view":"none","amenities":["WiFi","safe","AC","smart_tv"],"size_sqm":21,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/103.png","last_cleaned":"2026-04-06T10:15:00Z","maintenance_notes":null}'),
(104, '104', 11, 'AVAILABLE', 1, '{"room_id":"F1-104","wing":"Lobby Wing","bed_type":"Single","view":"courtyard","amenities":["WiFi","safe","AC","smart_tv"],"size_sqm":22,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/104.png","last_cleaned":"2026-04-06T06:20:00Z","maintenance_notes":null}'),

(201, '201', 11, 'OCCUPIED', 2, '{"room_id":"F2-201","wing":"Garden Wing","bed_type":"Single","view":"garden","amenities":["WiFi","safe","AC","minibar"],"size_sqm":24,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/201.png","last_cleaned":"2026-04-06T08:40:00Z","maintenance_notes":null}'),
(202, '202', 12, 'OCCUPIED', 2, '{"room_id":"F2-202","wing":"Garden Wing","bed_type":"Queen","view":"garden","amenities":["WiFi","minibar","safe","AC","balcony","espresso_machine"],"size_sqm":34,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/202.png","last_cleaned":"2026-04-06T07:50:00Z","maintenance_notes":null}'),

(301, '301', 12, 'OCCUPIED', 3, '{"room_id":"F3-301","wing":"Executive Wing","bed_type":"King","view":"city","amenities":["WiFi","minibar","safe","AC","balcony","espresso_machine","work_desk"],"size_sqm":38,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/301.png","last_cleaned":"2026-04-06T07:30:00Z","maintenance_notes":null}'),
(303, '303', 13, 'OCCUPIED', 3, '{"room_id":"F3-303","wing":"Executive Wing","bed_type":"King","view":"city","amenities":["WiFi","minibar","safe","AC","balcony","sofa_lounge","bathtub"],"size_sqm":48,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/303.png","last_cleaned":"2026-04-06T07:20:00Z","maintenance_notes":null}'),

(401, '401', 13, 'OCCUPIED', 4, '{"room_id":"F4-401","wing":"Premium Wing","bed_type":"King","view":"pool","amenities":["WiFi","minibar","safe","AC","balcony","sofa_lounge","bathtub","smart_control"],"size_sqm":54,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/401.png","last_cleaned":"2026-04-06T07:05:00Z","maintenance_notes":null}'),
(402, '402', 14, 'OCCUPIED', 4, '{"room_id":"F4-402","wing":"Premium Wing","bed_type":"King","view":"city","amenities":["WiFi","minibar","safe","AC","balcony","jacuzzi","dining_area","butler_call"],"size_sqm":72,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/402.png","last_cleaned":"2026-04-06T06:55:00Z","maintenance_notes":null}'),

(501, '501', 14, 'OCCUPIED', 5, '{"room_id":"F5-501","wing":"Penthouse Wing","bed_type":"King","view":"city","amenities":["WiFi","minibar","safe","AC","balcony","jacuzzi","dining_area","butler_call","private_bar"],"size_sqm":82,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/501.png","last_cleaned":"2026-04-06T06:50:00Z","maintenance_notes":null}'),
(515, '515', 15, 'OCCUPIED', 5, '{"room_id":"F5-515","wing":"Penthouse Wing","bed_type":"King","view":"city","amenities":["WiFi","minibar","safe","AC","private_terrace","jacuzzi","butler_service","kitchenette","private_dining"],"size_sqm":140,"smoking":false,"accessible":false,"floor_plan_url":"https://example.com/plans/515.png","last_cleaned":"2026-04-06T06:05:00Z","maintenance_notes":null}');

-- =========================================
-- GUESTS (map guest_id in doc_id: "G-xxxx|passport")
-- =========================================
INSERT OR REPLACE INTO guests (id, user_id, first_name, last_name, email, phone, doc_id) VALUES
(1, NULL, 'Amira', 'Hassan', 'amira.hassan@email.com', '+1-555-0192', 'G-0001|A12345678'),
(2, NULL, 'Luca', 'Bianchi', 'luca.bianchi@email.com', '+1-555-0193', 'G-0002|YA7788211'),
(3, NULL, 'Aiko', 'Tanaka', 'aiko.tanaka@email.com', '+1-555-0194', 'G-0003|TK3399172'),
(4, NULL, 'Kwame', 'Mensah', 'kwame.mensah@email.com', '+1-555-0195', 'G-0004|GH9981230'),
(5, NULL, 'Sofia', 'Rojas', 'sofia.rojas@email.com', '+1-555-0196', 'G-0005|CL6612045'),
(6, NULL, 'Omar', 'Al-Farsi', 'omar.alfarsi@email.com', '+1-555-0197', 'G-0006|OM5100291');

-- =========================================
-- RESERVATIONS (booking_source/payment fields can be stored in notes if needed)
-- =========================================
INSERT OR REPLACE INTO reservations
(id, guest_id, room_id, check_in_date, check_out_date, status, adults, children, created_at)
VALUES
(9001, 1, 303, '2026-04-05', '2026-04-10', 'CHECKED_IN', 2, 1, '2026-04-01T14:22:00Z'),
(9002, 2, 202, '2026-04-04', '2026-04-08', 'CHECKED_IN', 2, 0, '2026-03-28T11:10:00Z'),
(9003, 3, 401, '2026-04-02', '2026-04-07', 'CHECKED_IN', 1, 0, '2026-03-27T09:05:00Z'),
(9004, 4, 201, '2026-03-22', '2026-03-26', 'CHECKED_OUT', 1, 0, '2026-03-20T16:40:00Z'),
(9005, 5, 515, '2026-03-18', '2026-03-21', 'CHECKED_OUT', 2, 0, '2026-03-15T10:02:00Z'),
(9006, 6, 501, '2026-04-03', '2026-04-09', 'CHECKED_IN', 2, 1, '2026-03-30T08:40:00Z');

-- =========================================
-- INVOICES + PAYMENTS (for sample reservations)
-- =========================================
INSERT OR REPLACE INTO invoices
(id, reservation_id, guest_id, issue_date, due_date, total_amount, currency, status)
VALUES
(7001, 9001, 1, '2026-04-05', '2026-04-10', 1620.00, 'USD', 'ISSUED'),
(7002, 9002, 2, '2026-04-04', '2026-04-08', 864.00, 'USD', 'PAID');

INSERT OR REPLACE INTO invoice_lines (invoice_id, description, quantity, unit_price, line_total) VALUES
(7001, 'Room charge', 5, 360.00, 1800.00),
(7001, 'Discount', 1, -180.00, -180.00),
(7002, 'Room charge', 4, 240.00, 960.00),
(7002, 'Discount', 1, -96.00, -96.00);

INSERT OR REPLACE INTO payments (invoice_id, amount, method, paid_at, reference) VALUES
(7001, 1620.00, 'CARD', '2026-04-05T16:00:00Z', 'BK-20260401-001'),
(7002, 864.00,  'BANK_TRANSFER', '2026-04-04T18:10:00Z', 'BK-20260328-002');

COMMIT;