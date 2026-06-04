# SmartStay PMS - Entity Relationship Diagram (ERD) Description

## Core Entities and Relationships

### 1. `Hotel` (Multi-tenant root)
- **Attributes:** `id`, `name`, `address`, `city`, `country`, `contactEmail`, `contactPhone`, `starRating`
- **Relationships:**
  - `1:N` with `Room` (A hotel has many rooms)
  - `1:N` with `StaffProfile` (A hotel employs many staff members)
  - `1:N` with `Reservation` (A hotel has many reservations)

### 2. `UserAccount` & `Role` (Security & Auth)
- **UserAccount Attributes:** `id`, `email`, `passwordHash`, `firstName`, `lastName`, `isActive`
- **Role Attributes:** `id`, `name` (ADMIN, RECEPTIONIST, MANAGER, HOUSEKEEPING, CUSTOMER)
- **Relationships:**
  - `N:M` between `UserAccount` and `Role`
  - `1:1` with `Customer` (If the user is a guest)
  - `1:1` with `StaffProfile` (If the user is an employee)

### 3. `Customer` (CRM)
- **Attributes:** `id`, `phoneNumber`, `identityDocumentType`, `identityDocumentNumber`, `preferences`
- **Relationships:**
  - `1:N` with `Reservation` (A customer makes many reservations)
  - `1:1` with `LoyaltyAccount`

### 4. `Room` & `RoomType`
- **Room Attributes:** `id`, `roomNumber`, `floor`, `status` (AVAILABLE, OCCUPIED, CLEANING, MAINTENANCE)
- **RoomType Attributes:** `id`, `name` (SINGLE, DOUBLE, SUITE), `basePrice`, `capacity`, `description`
- **Relationships:**
  - `N:1` `Room` to `RoomType`
  - `1:N` `Room` to `Reservation` (A room is booked many times)
  - `1:N` `Room` to `MaintenanceTicket`

### 5. `Reservation` (Core Booking)
- **Attributes:** `id`, `checkInDate`, `checkOutDate`, `status` (PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED), `totalAmount`, `adults`, `children`, `specialRequests`, `confirmationCode`
- **Relationships:**
  - `N:1` to `Hotel`
  - `N:1` to `Customer`
  - `N:1` to `Room`
  - `1:1` to `Invoice`
  - `1:N` to `Payment`

### 6. `Invoice` & `InvoiceLineItem` & `Payment` (Billing)
- **Invoice Attributes:** `id`, `issueDate`, `dueDate`, `subtotal`, `taxAmount`, `totalAmount`, `status`
- **InvoiceLineItem Attributes:** `id`, `description`, `quantity`, `unitPrice`, `totalPrice`
- **Payment Attributes:** `id`, `amount`, `paymentDate`, `paymentMethod` (CASH, CARD, ONLINE), `status`
- **Relationships:**
  - `1:1` `Reservation` to `Invoice`
  - `1:N` `Invoice` to `InvoiceLineItem`
  - `1:N` `Reservation` to `Payment` (A reservation can have multiple payments/deposits)

### 7. `StaffProfile` & `Shift` & `MaintenanceTicket`
- **StaffProfile Attributes:** `id`, `employeeCode`, `department`, `hireDate`
- **Shift Attributes:** `id`, `startTime`, `endTime`, `status`, `notes`
- **MaintenanceTicket Attributes:** `id`, `description`, `priority`, `status`, `reportedAt`, `resolvedAt`
- **Relationships:**
  - `1:N` `StaffProfile` to `Shift`
  - `1:N` `StaffProfile` to `MaintenanceTicket` (assigned to)
