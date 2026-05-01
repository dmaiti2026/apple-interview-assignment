# Apple Interview Assignment

A Spring Boot REST API with a single-page frontend for task creation and approval workflow.

---


## Prerequisites

- **Java 17+** — `java -version`
- **Maven 3.6+** — `mvn -version`

No database setup needed — H2 runs fully in-memory.

---

## Project Structure

```
apple-interview-assignment
├── pom.xml
└── src/
    └── main/
        ├── java/com/apple/taskmanager/
        │   ├── TaskManagerApplication.java       # Entry point
        │   ├── config/
        │   │   ├── DataInitializer.java          # Seeds demo users & tasks on startup
        │   │   └── SecurityConfig.java           # Spring Security 
        │   ├── controller/
        │   │   ├── AuthController.java           # POST /api/auth/login|logout, GET /api/auth/me
        │   │   ├── TaskController.java           # CRUD + approve + stats + CSV export
        │   │   └── UserController.java           # GET /api/users
        │   ├── dto/
        │   │   ├── ApprovalRequest.java
        │   │   ├── LoginRequest.java
        │   │   ├── TaskRequest.java
        │   │   ├── TaskResponse.java
        │   │   └── UserResponse.java
        │   ├── entity/
        │   │   ├── Priority.java                 # Enum: LOW | MEDIUM | HIGH
        │   │   ├── Role.java                     # Enum: USER | MANAGER | ADMIN
        │   │   ├── Task.java
        │   │   ├── TaskStatus.java               # Enum: PENDING | APPROVED | REJECTED
        │   │   └── User.java
        │   ├── repository/
        │   │   ├── TaskRepository.java
        │   │   └── UserRepository.java
        │   └── service/
        │       ├── TaskService.java
        │       └── UserDetailsServiceImpl.java
        └── resources/
            ├── application.properties
            └── static/
                ├── index.html                    # Single-page app
                ├── css/style.css
                └── js/app.js                     # All frontend logic
```

---

## Setup & Run

### 1. Navigate to the project directory

```bash
cd apple-interview-assignment
```

### 2. Build

```bash
mvn clean package
```

### 3. Run

```bash
java -jar target/task-manager-1.0.0.jar
```

### 4. Open the app

```
http://localhost:8080
```

H2 console (for inspection):

```
http://localhost:8080/h2-console
JDBC URL:  jdbc:h2:mem:taskmanagerdb
Username:  sa
Password:  (empty)
```

---

## Demo Credentials

| Username   | Password     | Role    | Capabilities                                  |
| ---------- | ------------ | ------- | --------------------------------------------- |
| `admin`    | `admin123`   | ADMIN   | Full access — create, view, approve, reject   |
| `manager1` | `manager123` | MANAGER | Create tasks, approve/reject any pending task |
| `manager2` | `manager123` | MANAGER | Create tasks, approve/reject any pending task |
| `user1`    | `user123`    | USER    | Create tasks, view all tasks                  |
| `user2`    | `user123`    | USER    | Create tasks, view all tasks                  |
| `user3`    | `user123`    | USER    | Create tasks, view all tasks                  |

For demo purpose 10 sample tasks are pre-loaded across all statuses on startup.

---


---

## Workflow Logic

### Task states

```
[Created] ──► PENDING ──► APPROVED
                     └──► REJECTED
```

Status transitions are one-way — once a task is approved or rejected it cannot be changed.

### Task creation

- Any authenticated user can create a task.
- Required: **Title**, **Date/Time**, **Priority** (`LOW` / `MEDIUM` / `HIGH`).
- Optional: Description, Assigned User.
- Status is set to `PENDING` automatically.
- A console notification is printed on creation:
  ```
  [NOTIFICATION] New task created: 'Q2 Product Review' (ID: 1) by Alice Smith | Status: PENDING | Priority: HIGH
  ```

### Approval / rejection

- Only **MANAGER** or **ADMIN** can call `PUT /api/tasks/{id}/approve`.
- `reviewedBy` and `reviewedDate` are stamped on the record.
- Optional comments are persisted and displayed in the UI.
- Console notifications are emitted:
  ```
  [NOTIFICATION] Task 'Q2 Product Review' (ID: 1) PENDING -> APPROVED | Reviewed by: Sarah Johnson | Comments: Looks great!
  [NOTIFICATION -> Alice Smith] Your task 'Q2 Product Review' has been APPROVED
  ```

---

## Role-Based Access Control

| Action                        | USER | MANAGER | ADMIN |
| ----------------------------- | :--: | :-----: | :---: |
| Login / Logout                | ✓    | ✓       | ✓     |
| View all tasks                | ✓    | ✓       | ✓     |
| Create task                   | ✓    | ✓       | ✓     |
| Approve / Reject task         | ✗    | ✓       | ✓     |
| View "Pending Approvals" tab  | ✗    | ✓       | ✓     |
| Export CSV                    | ✓    | ✓       | ✓     |



RBAC is enforced at two levels:

1. **Spring Security** — `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")` on the approve endpoint.
2. **Frontend** — Approve/Reject buttons and the "Pending Approvals" tab are hidden for `USER` sessions.

---

## Frontend Features

| Feature               | Description                                                           |
| --------------------- | --------------------------------------------------------------------- |
| Login / Logout        | Session-based auth; 401 responses auto-redirect to login              |
| Dashboard             | Live stat cards (total / pending / approved / rejected)               |
| Task List             | Filterable by status; sortable by date or priority                    |
| Pending Approvals tab | One-click approve / reject with optional comment modal                |
| Create Task           | Modal form with validation; user assignment dropdown                  |
| Calendar View         | Month grid with color-coded task dots by status; prev/next navigation |
| CSV Export            | Downloads current filter set as `tasks-export.csv`                   |
| Toast notifications   | Non-blocking success/error feedback on every action                   |
| Keyboard shortcuts    | `Esc` closes any open modal                                           |