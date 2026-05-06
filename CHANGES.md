# SmartStay PMS — Rebuild & Bug-Fix Log

## 1. Bug Fixes

### 1.1 — Critical NullPointerExceptions (NPEs)
- **LoginController**: Fixed `insertGuest()` to link new guests to their user accounts via `user_id`. (Requires schema update).
- **AdminController**: Added null-guards to `sideNavButtons()` to prevent NPEs when specific FXML components are missing.
- **AdminController**: Added `d.getNode() != null` checks in `buildClientChart()` to prevent NPEs during asynchronous chart node creation.
- **StaffProfileController**: Replaced hardcoded weekly shifts with a real database query for the current week.
- **HomeController**: Provided default dates (today/tomorrow) in `openRoomDetail()` to prevent NPEs when date pickers are empty.
- **PaymentController**: Implemented a Linux fallback for `Desktop.open()` using an informative `Alert` with a copyable file path.
- **SecurityQuestionDao**: Removed dead loop in `saveAnswers()` that caused confusion and redundant logic.

### 1.2 — FXML / Controller Wiring
- **payment.fxml & room_detail.fxml**: Wrapped in `StackPane + MediaView` structure for video background consistency; registered in their respective controllers.
- **login.fxml**: Verified `onMouseClicked="#chooseRegAvatar"` for the avatar pane.
- **admin.fxml**: Verified `fx:id="liveResRows"` for the live reservation stream.
- **reception.fxml**: Verified `fx:id="lblSelectedRes"` for the reservation selection indicator.
- **staff_profile.fxml**: Verified `fx:id="shiftsContainer"` for the weekly roster grid.
- **Profile FXMLs**: Ensured all expandable card bodies start as `visible="false" managed="false"` for a collapsed initial state.

### 1.3 — Logic & Data
- **ReservationDao**: Fixed `findByGuest(userId)` to include both direct bookings and reservations where the guest email matches the user email (linking walk-ins).
- **AdminController**: Updated `loadOverview()` and `buildRoomStatusChart()` to use `Room.Status.CLEANING.ordinal()` instead of hardcoded indices.
- **AdminController**: Removed manual `-fx-bar-fill` inline styles in charts, deferring to `samurai.css`.
- **Cleaning/Maintenance DAOs**: Updated `findStaffProfileId` to accept `long userId` to match the `User` model.
- **LoginController**: Removed redundant `findActiveUser` network query after registration, building the `User` object from local state instead.
- **PayrollDao**: Fixed `generateForPeriod()` query to only include active staff accounts (`u.is_active = TRUE`).

### 1.4 — PDF & Unicode
- **InvoiceExportService**: Replaced non-ASCII characters ("侍", emojis) with compatible alternatives ("S", "[ROOM]", "[SVC]") to ensure correct rendering with standard PDF fonts.

## 2. New Features

### 2.1 — Client Cancel Booking
- Added a "Cancel Booking" button to reservation cards in the `HomeController` bookings tab.
- Includes a confirmation dialog and atomic status updates via `ReservationDao`.

### 2.2 — Staff Duty Roster
- Implemented real-time schedule viewing for staff members in their profile.
- Displays the current week's shifts (Monday–Sunday) with "Day Off" fallbacks.

### 2.3 — Payroll Inline Edit
- Added Bonus and Deduction fields to the Admin Payroll tab.
- Admins can now edit payroll entries inline and recalculate net salary automatically.

## 3. Code Quality & Refactoring

### 3.1 — Shared Utilities
- **CardToggleUtil**: Extracted identical `toggle()` animation logic from all profile controllers into a single utility.
- **CardBuilder**: Extracted shared `detailRow()` and `createBadge()` helpers used across multiple dashboards.

### 3.2 — Performance & Safety
- **ServiceExecutor**: Replaced all raw `new Thread().start()` calls with a centralized thread pool for better resource management.
- **HikariCP**: Integrated HikariCP in `Database.java` with a pool size of 5 for efficient connection management.
- **Media Cleanup**: Implemented `VideoBackground.cleanupDetached()` called via `Navigator` to prevent `MediaPlayer` leaks across scene transitions.

## 4. Database Schema
- **payroll**: Added UNIQUE KEY `uq_payroll_period (staff_profile_id, period_start, period_end)`.
- **staff_shift_assignments**: Added UNIQUE KEY `uq_shift_assign (staff_profile_id, assigned_date)`.
- **guests**: Added `user_id` column for direct linkage.
- **security_questions**: Verified existence and added 5 default questions.
- **services**: Added `is_active` column.
- **maintenance_requests**: Added `reported_by_user_id` column.