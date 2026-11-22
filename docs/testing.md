# HostelFixApp Testing Strategy & Documentation

This document outlines the testing architecture, test suites, and execution instructions for **HostelFixApp**.

---

## 1. Testing Architecture Overview

HostelFixApp follows a three-tier testing strategy following Android testing best practices:

```
                          ┌───────────────────────────┐
                          │   Compose UI Tests        │ (5 Instrumented UI Tests)
                          ├───────────────────────────┤
                          │   Integration Tests       │ (3 SQLite + Repository Tests)
                          ├───────────────────────────┤
                          │   Local Unit Tests        │ (35 ViewModel & Repository Tests)
                          └───────────────────────────┘
```

---

## 2. Test Categories & Suite Breakdown

### A. Local Unit Tests (`app/src/test/`) — 35 Tests
Local unit tests run on the host development machine JVM via Gradle. They use in-memory Fakes (`FakeReportRepository`, `FakeAuthRepository`, `FakeChatRepository`) for sub-second execution.

- **ViewModels**:
  - `StudentViewModelTest.kt` — Submit validations, active vs archived report filtering, resolution confirmations, message resetting.
  - `StaffViewModelTest.kt` — Unassigned staff status blocking (`IN_PROGRESS`, `FIXED`), staff assignments, status filter, hostel filter, audit logs.
  - `AuthViewModelTest.kt` — Login, Sign Up, Password Reset, Profile picture updates, Logout session management.
  - `ChatViewModelTest.kt` — Real-time chat messages, offline queueing, auto-sync when reconnected.
- **Repositories**:
  - `ReportRepositoryTest.kt` — CRUD operations, staff assignments, status timeline updates.
  - `AuthRepositoryTest.kt` — Account authentication, profile updates.
  - `ChatRepositoryTest.kt` — Real-time messaging and offline sync.

---

### B. Integration Tests (`app/src/androidTest/`) — 3 Tests
Integration tests run on an Android device or emulator to test real SQLite database engine persistence and repository data flows.

- **`ReportRepositoryImplIntegrationTest.kt`** — Tests `ReportRepositoryImpl` integrated directly with real `HostelFixLocalDb` SQLite database engine.
- **`HostelFixLocalDbTest.kt`** — Tests SQLite table creation, user CRUD, complaint persistence, and chat message storage.

---

### C. Compose UI Instrumented Tests (`app/src/androidTest/`) — 5 Tests
UI tests run on an Android device or emulator to test screen rendering and UI interactions using Compose Test Rules.

- **`LoginScreenTest.kt`** — Tests email/password text fields, sign in button, links, and text input behavior.
- **`StudentHomeScreenTest.kt`** — Tests Student Portal header, filter buttons (`All`, `Pending`, `In Progress`, `Fixed`), and FAB navigation.
- **`StaffDashboardScreenTest.kt`** — Tests Maintenance Office Portal header, logged-in staff info, and filter chips.

---

## 3. How to Execute Tests

### A. Running Local Unit Tests (35 Tests)

#### Via Gradle Command Line:
```powershell
.\gradlew testDebugUnitTest
```

#### Via Android Studio IDE:
1. Expand `app` ➔ `src` ➔ `test` ➔ `java` ➔ `com.example.hostelfixapp`.
2. Right-click the `ui` or `repository` folder ➔ Click **Run 'Tests in...'** (or press `Ctrl + Shift + F10`).

---

### B. Running Instrumented Integration & UI Tests (Device / Emulator required)

#### Via Gradle Command Line:
```powershell
.\gradlew connectedCheck
```

#### Via Android Studio IDE:
1. Ensure an Android emulator or physical device is connected.
2. Expand `app` ➔ `src` ➔ `androidTest` ➔ `java` ➔ `com.example.hostelfixapp`.
3. Right-click the `ui` or `data` folder ➔ Click **Run 'Tests in...'**.

---

## 4. Test Fakes Reference

Test fakes live in `app/src/test/java/com/example/hostelfixapp/fakes/` (for unit tests) and `app/src/androidTest/java/com/example/hostelfixapp/fakes/` (for instrumented tests):

- **`FakeReportRepository`** — In-memory report list and status timeline storage.
- **`FakeAuthRepository`** — In-memory user session and authentication state flow.
- **`FakeChatRepository`** — In-memory message list and simulated online/offline state.
