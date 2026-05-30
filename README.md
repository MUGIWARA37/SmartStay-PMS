# SmartStay PMS

> **Property Management System** — JavaFX 21 desktop application for full-cycle hotel operations.  
> Academic project · ENSA Khouribga

---

## Overview

SmartStay PMS centralises every aspect of hotel management into a single, role-aware desktop application. Receptionists check guests in and out, cleaning and maintenance staff action their task queues, and administrators oversee revenue, payroll, and occupancy — all from one cohesive interface backed by a MySQL database.

---

## Tech Stack

| Layer         | Technology                        |
|---------------|-----------------------------------|
| Language      | Java 17                           |
| UI Framework  | JavaFX 21 (FXML + CSS)            |
| Database      | MySQL 8 (Docker Compose)          |
| Build         | Maven 3.9                         |
| Fonts         | Playfair Display · DM Sans (Google Fonts) |

---

## Features

### Authentication & Security
- Role-based login: Admin, Reception, Cleaning, Maintenance, Guest (CLIENT)
- Account registration with profile photo upload
- 3-question security challenge for password recovery
- BCrypt password hashing

### Room Management
- Room types with per-night pricing, floor, occupancy, and amenities
- Real-time status board: Available · Occupied · Cleaning · Maintenance
- Admin bulk-status update with filter view

### Reservation Lifecycle
- Guest-facing room browsing with filters (type, price range, dates)
- Date conflict detection and availability check
- Full booking flow: room detail → guest info → add-on services → payment card → confirmation
- PDF receipt download on confirmation
- Receptionist walk-in registration and check-in / check-out actions

### Billing & Invoicing
- Per-reservation invoice with line items (room + services)
- Tax calculation and grand total
- Payment status tracking (Pending → Paid)

### Staff Management
- Staff profiles linked to user accounts (employee code, department, hire date, base salary)
- Shift assignment with date picker and notes
- Activate / deactivate employees

### Payroll
- Period-based payroll generation (base + bonuses − deductions = net)
- Mark-as-paid workflow
- Admin Revenue Chart — annual bar chart by month

### Admin Dashboard
- 4 KPI stat cards: Total Rooms · Active Guests · Today's Check-ins · Monthly Revenue
- Room Status donut chart
- Monthly Check-ins bar chart
- Legion Productivity panel with progress bars
- Live Reservation Stream table with quick-navigate link

### Operational Dashboards
- **Reception:** Reservations grid · Walk-in · Cleaning dispatch · Maintenance dispatch
- **Cleaning:** Task queue by room and floor
- **Maintenance:** Problem reports with category and priority

### Profiles
- Expandable accordion cards: Profile Hero · Communications · Authentication · Session Info
- In-app profile photo change
- Password change with current-password verification

---

## Project Structure

```
SmartStay-PMS/
├── pom.xml
├── README.md
├── docker-compose.yml
└── src/main/
    ├── java/ma/ensa/khouribga/smartstay/
    │   ├── Main.java                  ← JavaFX launcher
    │   ├── MainApp.java               ← Application bootstrap
    │   ├── Navigator.java             ← Scene-switching utility
    │   ├── ThemeManager.java          ← Dark / Light toggle
    │   ├── VideoBackground.java       ← Looping video layer
    │   ├── LandingController.java
    │   ├── admin/                     ← Admin dashboard
    │   ├── auth/                      ← Login, register, recovery
    │   ├── dao/                       ← Data Access Objects (JDBC)
    │   ├── db/                        ← Connection pool, TxManager
    │   ├── guest/                     ← Room detail, payment
    │   ├── home/                      ← Guest portal, room cards
    │   ├── model/                     ← Plain Java models
    │   ├── profile/                   ← Admin/Staff/Client profiles
    │   ├── service/                   ← Business logic layer
    │   ├── session/                   ← SessionManager
    │   ├── staff/                     ← Reception, Cleaning, Maintenance
    │   └── util/                      ← AlertUtil, CardBuilder, etc.
    └── resources/
        ├── application.properties
        ├── fxml/                      ← FXML layouts per role/view
        ├── images/rooms/              ← Room type photos
        ├── sql/
        │   ├── schema.sql             ← Full DDL
        │   ├── seed.sql               ← Sample data
        │   └── gen_seed.py            ← Seed generator script
        ├── styles/
        │   └── style.css              ← Unified theme (dark + light)
        └── videos/                    ← Background loop videos
```

---

## Database Setup

MySQL 8 runs in Docker — no local installation required.

```bash
# Start the database
docker compose up -d

# Stop the database
docker compose down
```

The schema and seed data are loaded automatically on first start via `docker-entrypoint-initdb.d`.

Connection settings live in `src/main/resources/application.properties`.

---

## Running the Application

**Prerequisites:** Java 17 · Maven 3.9+ · Docker

```bash
# 1 — Start the database
docker compose up -d

# 2 — Run the application
mvn javafx:run
```

To load optional Google Fonts (Playfair Display, DM Sans), add the following to `MainApp.java` before the primary stage shows:

```java
Font.loadFont(
    MainApp.class.getResourceAsStream("/fonts/PlayfairDisplay-Bold.ttf"), 14);
Font.loadFont(
    MainApp.class.getResourceAsStream("/fonts/DMSans-Regular.ttf"), 14);
```

Or simply keep the existing fallback fonts — the UI degrades gracefully.

---

## User Roles & Access

| Role                   | Portal                              | Key Capabilities                                     |
|------------------------|-------------------------------------|------------------------------------------------------|
| `ADMIN`                | Admin Dashboard                     | Full access — rooms, reservations, payroll, staff    |
| `STAFF` — Reception    | Reception Dashboard                 | Check-in/out, walk-in, cleaning & maintenance dispatch |
| `STAFF` — Cleaning     | Cleaning Dashboard                  | Task queue by room and floor                         |
| `STAFF` — Maintenance  | Maintenance Dashboard               | Problem report queue with priority and category      |
| `CLIENT`               | Guest Portal                        | Browse rooms, book, view reservations, pay           |

---

## CSS Theme System

All colours are defined as CSS custom properties on `.root` (dark, default) and `.light-mode` (light). Toggling themes is a single class swap on the root node via `ThemeManager`.

Design tokens follow a naming convention:

```
-ss-bg-*        Background scale (base → surface → elevated → raised)
-ss-text-*      Text scale (primary → secondary → tertiary)
-ss-gold        Brand accent — Champagne Gold
-ss-border-*    Border opacity scale
-ss-chart-*     Chart series colours
```

Font stack:
- **Display (headings):** Playfair Display → Georgia → Times New Roman
- **Body (UI text):**     DM Sans → Segoe UI → Helvetica Neue

---

## Author

ENSA Khouribga — SmartStay PMS Academic Project · 2025