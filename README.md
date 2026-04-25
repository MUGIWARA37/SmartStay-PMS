# SmartStay PMS

**SmartStay PMS** (Property Management System) is a Java desktop application designed to simplify hotel operations such as room management, guest management, reservations, check-in/check-out, and billing/invoicing.

This project is developed as an academic project at **ENSA Khouribga** and focuses on clean architecture, maintainable code, and a modular feature-based structure.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| UI | JavaFX 21 |
| Database | MySQL 8 (Docker) |
| Build tool | Maven 3.9 |
| Testing | JUnit (planned) |

---

## Features

- **Authentication** — Role-based login (Admin, Reception, Cleaning, Maintenance)
- **Room management** — Room types, status tracking, pricing
- **Guest management** — Guest profiles, document ID, history
- **Reservation management** — Availability search, booking, check-in/check-out
- **Billing & invoicing** — Services/extras, payments, invoice generation
- **Staff management** — Shifts, attendance, payroll
- **Admin dashboard** — Occupancy stats, revenue graphs, employee rankings
- **Cleaning dashboard** — Rooms and floors pending cleaning
- **Maintenance dashboard** — Problem reports by location

---

## Project Structure

```text
smartstay-pms/
├── pom.xml
├── README.md
├── docker-compose.yml
├── .gitignore
└── src/
   ├── main/
   │  ├── java/
   │  │  └── ma/ensa/khouribga/smartstay/
   │  │     ├── MainApp.java
   │  │     ├── db/
   │  │     │  ├── Database.java
   │  │     │  └── TxManager.java
   │  │     ├── session/
   │  │     │  └── SessionManager.java
   │  │     ├── model/
   │  │     │  ├── User.java
   │  │     │  ├── Room.java
   │  │     │  ├── Reservation.java
   │  │     │  ├── Service.java
   │  │     │  └── Payroll.java
   │  │     ├── auth/
   │  │     │  └── LoginController.java
   │  │     ├── home/
   │  │     │  └── HomeController.java
   │  │     ├── guest/
   │  │     │  ├── RoomDetailController.java
   │  │     │  └── PaymentController.java
   │  │     ├── staff/
   │  │     │  ├── CleaningController.java
   │  │     │  ├── MaintenanceController.java
   │  │     │  └── ReceptionController.java
   │  │     └── admin/
   │  │        └── AdminController.java
   │  └── resources/
   │     ├── application.properties
   │     ├── fxml/
   │     │  ├── auth/
   │     │  │  └── login.fxml
   │     │  ├── home/
   │     │  │  └── home.fxml
   │     │  ├── guest/
   │     │  │  ├── room_detail.fxml
   │     │  │  └── payment.fxml
   │     │  ├── staff/
   │     │  │  ├── cleaning.fxml
   │     │  │  ├── maintenance.fxml
   │     │  │  └── reception.fxml
   │     │  └── admin/
   │     │     └── admin.fxml
   │     ├── styles/
   │     │  └── samurai.css
   │     ├── images/
   │     └── sql/
   │        ├── schema.sql
   │        └── seed.sql
   └── test/
      └── java/
         └── ma/ensa/khouribga/smartstay/
```

---

## Database Setup

The project uses **MySQL 8** running in Docker. No manual installation needed.

```bash
# Start the database
docker compose up -d

# Stop the database
docker compose down
```

The schema and seed data are loaded automatically on first start via `docker-entrypoint-initdb.d`.

---

## How to Run

**Prerequisites:**
- Java 17
- Maven 3.9+
- Docker

```bash
# 1. Start the database
docker compose up -d

# 2. Run the app
mvn javafx:run
```

---

## User Roles

| Role | Access |
|---|---|
| `ADMIN` | Full dashboard, staff management, billing, rooms |
| `STAFF` (Reception) | Guest lookup, check-in/out, maintenance requests |
| `STAFF` (Cleaning) | Cleaning task list by room and floor |
| `STAFF` (Maintenance) | Problem reports by location |
| `CLIENT` | Home page, room browsing, booking, payment |

---

## Author

ENSA Khouribga — SmartStay PMS Academic Project