# SmartStay PMS

**SmartStay PMS** (Property Management System) is a Java desktop application designed to simplify hotel operations such as room management, guest management, reservations, check-in/check-out, and billing/invoicing.  
This project is developed as an academic project (ENSA Khouribga) and focuses on clean architecture, maintainable code, and a modular structure.

## Tech Stack
- **Language:** Java
- **UI:** JavaFX (Desktop)
- **Database:** SQL (e.g., MySQL or SQLite)
- **Build Tool:** Maven
- **Testing:** JUnit (planned)

## Main Features (Planned)
- Room management (room types, status, pricing)
- Guest management
- Reservation management (availability, booking, check-in/check-out)
- Billing and invoices (services/extras, payments)
- User authentication and roles (Admin / Receptionist)
- Dashboard (occupancy and revenue statistics)

## Project Structure

```text
smartstay-pms/
├─ pom.xml
├─ README.md
├─ .gitignore
└─ src/
   ├─ main/
   │  ├─ java/
   │  │  └─ ma/ensa/khouribga/smartstay/
   │  │     ├─ MainApp.java
   │  │     ├─ app/
   │  │     │  ├─ navigation/
   │  │     │  └─ session/
   │  │     ├─ config/
   │  │     ├─ db/
   │  │     │  ├─ Database.java
   │  │     │  ├─ TxManager.java
   │  │     │  └─ migration/
   │  │     ├─ common/
   │  │     │  ├─ exception/
   │  │     │  ├─ util/
   │  │     │  └─ validation/
   │  │     ├─ feature/
   │  │     │  ├─ auth/
   │  │     │  │  ├─ ui/
   │  │     │  │  ├─ service/
   │  │     │  │  ├─ dao/
   │  │     │  │  └─ model/
   │  │     │  ├─ room/
   │  │     │  │  ├─ ui/
   │  │     │  │  ├─ service/
   │  │     │  │  ├─ dao/
   │  │     │  │  └─ model/
   │  │     │  ├─ guest/
   │  │     │  │  ├─ ui/
   │  │     │  │  ├─ service/
   │  │     │  │  ├─ dao/
   │  │     │  │  └─ model/
   │  │     │  ├─ reservation/
   │  │     │  │  ├─ ui/
   │  │     │  │  ├─ service/
   │  │     │  │  ├─ dao/
   │  │     │  │  └─ model/
   │  │     │  ├─ billing/
   │  │     │  │  ├─ ui/
   │  │     │  │  ├─ service/
   │  │     │  │  ├─ dao/
   │  │     │  │  └─ model/
   │  │     │  └─ dashboard/
   │  │     │     ├─ ui/
   │  │     │     ├─ service/
   │  │     │     └─ dao/
   │  │     └─ report/
   │  │        ├─ export/
   │  │        └─ template/
   │  └─ resources/
   │     ├─ application.properties
   │     ├─ fxml/
   │     │  ├─ auth/
   │     │  ├─ room/
   │     │  ├─ guest/
   │     │  ├─ reservation/
   │     │  ├─ billing/
   │     │  └─ dashboard/
   │     ├─ css/
   │     ├─ images/
   │     └─ sql/
   │        ├─ schema.sql
   │        └─ seed.sql
   └─ test/
      └─ java/
         └─ ma/ensa/khouribga/smartstay/
            ├─ feature/
            │  ├─ reservation/service/
            │  └─ billing/service/
            └─ db/
```

## How to Run (Planned)
This section will be updated after configuring JavaFX + DB dependencies and creating the first working screens.

## Author
- ENSA Khouribga — SmartStay PMS Project