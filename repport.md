# SmartStay PMS Codebase Report

SmartStay PMS is a Java 17/21 desktop property management system that combines a JavaFX UI with a Spring Boot backend. The repository contains two parallel application stacks: a Spring Boot + JavaFX integrated stack under `com.smartstay.pms`, and a legacy/standalone JavaFX + JDBC stack under `ma.ensa.khouribga.smartstay`. They share UI assets (FXML, CSS, images, videos) but use different data access layers and database schemas.

## Repository layout

| Path | Purpose |
| --- | --- |
| `README.md` | Product overview, features, structure, run instructions. |
| `pom.xml` | Maven build, dependencies for JavaFX, Spring Boot, Security, JPA, JWT, MapStruct, OpenPDF, jBCrypt, tests. |
| `Makefile` | Developer workflows: build, run, run-api, test, db-shell. |
| `Dockerfile` | Builds a runnable Spring Boot jar and runs it in a JRE image. |
| `docker-compose.yml` | Starts `smartstay-app` and MySQL `smartstay-db`. |
| `ERD.md` | Conceptual ERD for multi-tenant hotel model (Hotel, UserAccount, Room, etc). |
| `src/main/java/com/smartstay/pms` | Spring Boot backend and Spring-managed JavaFX UI. |
| `src/main/java/ma/ensa/khouribga/smartstay` | Legacy JavaFX + JDBC app with DAOs. |
| `src/main/resources` | FXML views, CSS, SQL schema/seed, assets (images/videos). |
| `src/test` | JUnit tests and test profile configuration. |

## Architecture overview

### Spring Boot + JavaFX stack (`com.smartstay.pms`)

**Entry point and lifecycle**

1. `SmartStayApplication` (Spring Boot) launches `JavafxApplication`.
2. `JavafxApplication` creates the Spring context, then publishes `StageReadyEvent`.
3. `StageInitializer` receives the event, wires the JavaFX `Stage`, and navigates to `/fxml/landing.fxml` using `NavigationService`.
4. `ThemeManager` applies the initial theme to the scene.

**UI integration with Spring**

`NavigationService` loads FXML with `FXMLLoader` configured to get controllers from the Spring `ApplicationContext`. This means UI controllers are Spring beans and can inject services and repositories.

**Shared UI behavior**

`BaseUIController` is the common base for UI controllers. It registers video backgrounds (if present), provides theme toggling, and exposes the current toggle label.

### Legacy JavaFX + JDBC stack (`ma.ensa.khouribga.smartstay`)

This stack is a standalone JavaFX application with DAOs and services that access MySQL via JDBC.

**Entry point and lifecycle**

`Main` -> `MainApp` loads `/fxml/landing.fxml`, installs a global exception handler, and applies theme via `com.smartstay.pms.ui.util.ThemeManager`.

**Data access**

DAO classes use `Database.getConnection()` for JDBC access. `DatabaseInitializer` can apply SQL schema and seed data on first start and enforces required tables. This stack uses the SQL schema in `src/main/resources/sql`.

## Spring Boot backend package map (`com.smartstay.pms`)

### Core

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `com.smartstay.pms` | `SmartStayApplication`, `JavafxApplication`, `StageInitializer` | Spring Boot + JavaFX bootstrapping. |

### Configuration

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `com.smartstay.pms.config` | `AuditorAwareConfig`, `DataSeeder`, `OpenApiConfig`, `PricingProperties`, `TaxProperties` | Auditing, initial seed data, Swagger config, pricing/tax config. |

### Security

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `com.smartstay.pms.security` | `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `UserPrincipal`, `JwtProperties`, `SecurityUtils` | Stateless JWT security using Spring Security and bcrypt. |

### REST API and error handling

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `com.smartstay.pms.api` | `ApiResponse`, `GlobalExceptionHandler` | Unified API response envelope and centralized exception handling. |
| `com.smartstay.pms.controller` | `AuthController`, `CustomerController`, `DashboardController`, `PaymentController`, `ReservationController`, `RoomController` | REST endpoints for auth, rooms, reservations, payments, and analytics. |

### Services and business logic

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `com.smartstay.pms.service` | `AuthService`, `ReservationService`, `RoomService`, `PaymentService`, `DashboardService`, `CustomerService`, `AuditService` | Core business workflows and validation. |

### Data model and persistence

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `com.smartstay.pms.model` | `Hotel`, `UserAccount`, `Room`, `Reservation`, `Invoice`, `Payment`, `StaffProfile`, `Shift`, `MaintenanceTicket`, etc. | JPA entities with soft delete and auditing. |
| `com.smartstay.pms.repository` | `HotelRepository`, `RoomRepository`, `ReservationRepository`, `PaymentRepository`, etc. | Spring Data repositories with custom queries. |
| `com.smartstay.pms.mapper` | `RoomMapper`, `ReservationMapper`, `PaymentMapper`, `CustomerMapper`, etc. | MapStruct DTO mapping. |
| `com.smartstay.pms.dto` | `RoomResponse`, `ReservationResponse`, `AuthRequest`, etc. | API input/output contracts. |

### JavaFX UI controllers (Spring-managed)

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `com.smartstay.pms.ui` | `BaseUIController`, `LandingController` | Shared controller behavior and landing view. |
| `com.smartstay.pms.ui.auth` | `LoginController` | Login, register, recovery panels; routes to role-specific dashboards. |
| `com.smartstay.pms.ui.admin` | `AdminController` | Admin dashboard, KPI charts, room/reservation panels. |
| `com.smartstay.pms.ui.staff` | `ReceptionController` | Staff-facing reception dashboard. |
| `com.smartstay.pms.ui.home` | `HomeController`, `RoomCardController` | Guest home view and room card rendering. |
| `com.smartstay.pms.ui.service` | `NavigationService` | Spring-aware FXML navigation. |
| `com.smartstay.pms.ui.util` | `Navigator`, `ThemeManager`, `VideoBackground` | Navigation helpers, theme toggling, and video backgrounds. |

## Legacy JavaFX + JDBC package map (`ma.ensa.khouribga.smartstay`)

### Core and session

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `ma.ensa.khouribga.smartstay` | `Main`, `MainApp`, `LandingController` | Standalone JavaFX bootstrap and landing view. |
| `ma.ensa.khouribga.smartstay.session` | `SessionManager` | Current user session state and role checks. |

### Data access

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `ma.ensa.khouribga.smartstay.db` | `Database`, `DatabaseInitializer`, `TxManager` | JDBC connection, schema/seed setup, transaction helpers. |
| `ma.ensa.khouribga.smartstay.dao` | `ReservationDao`, `RoomDao`, `UserDao`, `InvoiceDao`, `CleaningDao`, `MaintenanceDao`, `PayrollDao`, `SecurityQuestionDao`, etc. | SQL queries for each domain table. |

### Services and UI

| Package | Key classes | Responsibility |
| --- | --- | --- |
| `ma.ensa.khouribga.smartstay.service` | `ReservationService`, `RoomService`, `InvoiceService`, `PayrollService`, `MaintenanceService`, etc. | Thin service layer over DAOs. |
| `ma.ensa.khouribga.smartstay.staff` | `ReceptionController`, `CleaningController`, `MaintenanceController` | Staff dashboards for operations. |
| `ma.ensa.khouribga.smartstay.admin` | `AdminController` | Admin dashboard for reports and management. |
| `ma.ensa.khouribga.smartstay.auth` | `LoginController` | Legacy login UI. |
| `ma.ensa.khouribga.smartstay.home` | `HomeController`, `RoomCardController` | Guest portal UI. |
| `ma.ensa.khouribga.smartstay.profile` | `AdminProfileController`, `StaffProfileController`, `ClientProfileController` | Profile screens. |
| `ma.ensa.khouribga.smartstay.util` | `AlertUtil`, `CardBuilder`, `ProfilePictureUtil`, `ServiceExecutor`, etc. | UI helpers and utilities. |

## API surface (Spring Boot)

All REST endpoints return `ApiResponse<T>` with `success`, `message`, and `data`.

| Controller | Base path | Key operations |
| --- | --- | --- |
| `AuthController` | `/api/v1/auth` | `POST /login` -> JWT token. |
| `RoomController` | `/api/v1/hotels/{hotelId}/rooms` | List, get, create, update rooms. |
| `ReservationController` | `/api/v1/hotels/{hotelId}/reservations` | List, get, create, update reservations. |
| `PaymentController` | `/api/v1/reservations/{reservationId}/payments` | List payments and create payment. |
| `DashboardController` | `/api/v1/hotels/{hotelId}/dashboard` | Occupancy and revenue reporting. |

## Data model and persistence

### Spring Boot JPA model

Entities live under `com.smartstay.pms.model` and map to tables prefixed with `pms_` (e.g., `pms_rooms`, `pms_reservations`, `pms_user_accounts`). `BaseEntity` provides UUID IDs, auditing fields, optimistic locking, and soft delete. The model reflects the ERD in `ERD.md` (Hotel, UserAccount, Room/RoomType, Reservation, Invoice, Payment, StaffProfile, Shift, MaintenanceTicket, etc).

### JDBC schema for the legacy app

`src/main/resources/sql/schema.sql` defines the non-prefixed tables used by the JDBC stack: `users`, `rooms`, `room_types`, `reservations`, `invoices`, `payments`, `services`, `staff_profiles`, `maintenance_requests`, `cleaning_requests`, `payroll`, and more. `DatabaseInitializer` can apply this schema and seed data.

### Seed data

| File | Purpose |
| --- | --- |
| `seed.sql` | Full data set spanning 2023-2026 with multiple staff and clients. |
| `seed_minimal.sql` | Small sample data set for fast local startup. |
| `DataSeeder` (Spring Boot) | Inserts default hotel, admin, staff, and rooms for JPA model. |

## UI resources

### FXML views

| Folder | Views |
| --- | --- |
| `fxml` | `landing.fxml` |
| `fxml/auth` | `login.fxml` |
| `fxml/home` | `home.fxml`, `room_card.fxml` |
| `fxml/guest` | `room_detail.fxml`, `payment.fxml` |
| `fxml/staff` | `reception.fxml`, `cleaning.fxml`, `maintenance.fxml` |
| `fxml/admin` | `admin.fxml` |
| `fxml/profile` | `admin_profile.fxml`, `staff_profile.fxml`, `client_profile.fxml` |

### Styling and assets

`styles/style.css` is the entry point and imports `tokens.css`, `base.css`, `navigation.css`, `components.css`, and `forms.css`. Theme switching is done by toggling the `light-mode` class on the root node. Background videos are in `resources/videos`, and room images are under `resources/images/rooms`.

## Configuration and runtime profiles

| File | Notes |
| --- | --- |
| `application.properties` | Default Spring Boot config (MySQL, JWT, Swagger, pricing/tax, virtual threads). |
| `application-dev.properties` | Dev overrides with env placeholders and Swagger enabled. |
| `application-prod.properties` | Production config, Swagger disabled, env-based DB/JWT settings. |
| `application-test.properties` | H2 in-memory DB and test JWT for unit/integration tests. |

## Build, run, and deployment

| Command | Effect |
| --- | --- |
| `mvn spring-boot:run -Dspring-boot.run.profiles=dev` | Runs Spring Boot with JavaFX UI. |
| `docker compose up -d` | Starts app and MySQL using `docker-compose.yml`. |
| `make run` | Starts DB and launches the desktop app. |
| `make run-api` | Runs app and DB via Docker (no GUI). |
| `make test` | Runs Maven tests. |

## Tests

| Test | Purpose |
| --- | --- |
| `BookingFlowIntegrationTest` | Spring context load test with test profile. |
| `ReservationServiceTest` | Unit tests for reservation creation and validation. |

## Notable implementation details

1. There are two parallel data access systems: Spring Boot JPA (prefixed `pms_` tables) and legacy JDBC (non-prefixed tables in `schema.sql`). This affects which DB schema is expected at runtime.
2. `Database` in the legacy stack expects `db.url` and `db.user` keys in `application.properties`, which are not present in the current file. The JDBC stack likely expects these keys to be added or passed via environment-specific configuration.
3. JavaFX controllers in the Spring stack are Spring-managed and can inject services directly, while the legacy stack uses plain controllers and DAOs.

